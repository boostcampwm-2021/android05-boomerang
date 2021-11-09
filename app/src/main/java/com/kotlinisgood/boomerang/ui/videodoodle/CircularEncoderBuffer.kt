package com.kotlinisgood.boomerang.ui.videodoodle

import android.media.MediaCodec
import android.util.Log
import java.lang.RuntimeException
import java.nio.ByteBuffer


/**
 * Holds encoded video data in a circular buffer.
 *
 *
 * This is actually a pair of circular buffers, one for the raw data and one for the meta-data
 * (flags and PTS).
 *
 *
 * Not thread-safe.
 */
class CircularEncoderBuffer(bitRate: Int, frameRate: Int, desiredSpanSec: Int) {
    // Raw data (e.g. AVC NAL units) held here.
    //
    // The MediaMuxer writeSampleData() function takes a ByteBuffer.  If it's a "direct"
    // ByteBuffer it'll access the data directly, if it's a regular ByteBuffer it'll use
    // JNI functions to access the backing byte[] (which, in the current VM, is done without
    // copying the data).
    //
    // It's much more convenient to work with a byte[], so we just wrap it with a ByteBuffer
    // as needed.  This is a bit awkward when we hit the edge of the buffer, but for that
    // we can just do an allocation and data copy (we know it happens at most once per file
    // save operation).
    private val mDataBufferWrapper: ByteBuffer
    private val mDataBuffer: ByteArray

    // Meta-data held here.  We're using a collection of arrays, rather than an array of
    // objects with multiple fields, to minimize allocations and heap footprint.
    private val mPacketFlags: IntArray
    private val mPacketPtsUsec: LongArray
    private val mPacketStart: IntArray
    private val mPacketLength: IntArray

    // Data is added at head and removed from tail.  Head points to an empty node, so if
    // head==tail the list is empty.
    private var mMetaHead = 0
    private var mMetaTail = 0

    /**
     * Computes the amount of time spanned by the buffered data, based on the presentation
     * time stamps.
     */
    fun computeTimeSpanUsec(): Long {
        val metaLen = mPacketStart.size
        if (mMetaHead == mMetaTail) {
            // empty list
            return 0
        }

        // head points to the next available node, so grab the previous one
        val beforeHead = (mMetaHead + metaLen - 1) % metaLen
        return mPacketPtsUsec[beforeHead] - mPacketPtsUsec[mMetaTail]
    }

    /**
     * Adds a new encoded data packet to the buffer.
     *
     * @param buf The data.  Set position() to the start offset and limit() to position+size.
     * The position and limit may be altered by this method.
     * @param size Number of bytes in the packet.
     * @param flags MediaCodec.BufferInfo flags.
     * @param ptsUsec Presentation time stamp, in microseconds.
     */
    fun add(buf: ByteBuffer, flags: Int, ptsUsec: Long) {
        val size = buf.limit() - buf.position()
        if (VERBOSE) {
            Log.d(
                TAG, "add size=" + size + " flags=0x" + Integer.toHexString(flags) +
                        " pts=" + ptsUsec
            )
        }
        while (!canAdd(size)) {
            removeTail()
        }
        val dataLen = mDataBuffer.size
        val metaLen = mPacketStart.size
        val packetStart = headStart
        mPacketFlags[mMetaHead] = flags
        mPacketPtsUsec[mMetaHead] = ptsUsec
        mPacketStart[mMetaHead] = packetStart
        mPacketLength[mMetaHead] = size

        // Copy the data in.  Take care if it gets split in half.
        if (packetStart + size < dataLen) {
            // one chunk
            buf[mDataBuffer, packetStart, size]
        } else {
            // two chunks
            val firstSize = dataLen - packetStart
            if (VERBOSE) {
                Log.v(
                    TAG,
                    "split, firstsize=$firstSize size=$size"
                )
            }
            buf[mDataBuffer, packetStart, firstSize]
            buf[mDataBuffer, 0, size - firstSize]
        }
        mMetaHead = (mMetaHead + 1) % metaLen
        if (EXTRA_DEBUG) {
            // The head packet is the next-available spot.
            mPacketFlags[mMetaHead] = 0x77aaccff
            mPacketPtsUsec[mMetaHead] = -1000000000L
            mPacketStart[mMetaHead] = -100000
            mPacketLength[mMetaHead] = Int.MAX_VALUE
        }
    }

    /**
     * Returns the index of the oldest sync frame.  Valid until the next add().
     *
     *
     * When sending output to a MediaMuxer, start here.
     */
    val firstIndex: Int
        get() {
            val metaLen = mPacketStart.size
            var index = mMetaTail
            while (index != mMetaHead) {
                if (mPacketFlags[index] and MediaCodec.BUFFER_FLAG_SYNC_FRAME != 0) {
                    break
                }
                index = (index + 1) % metaLen
            }
            if (index == mMetaHead) {
                Log.w(TAG, "HEY: could not find sync frame in buffer")
                index = -1
            }
            return index
        }

    /**
     * Returns the index of the next packet, or -1 if we've reached the end.
     */
    fun getNextIndex(index: Int): Int {
        val metaLen = mPacketStart.size
        var next = (index + 1) % metaLen
        if (next == mMetaHead) {
            next = -1
        }
        return next
    }

    /**
     * Returns a reference to a "direct" ByteBuffer with the data, and fills in the
     * BufferInfo.
     *
     *
     * The caller must not modify the contents of the returned ByteBuffer.  Altering
     * the position and limit is allowed.
     */
    fun getChunk(index: Int, info: MediaCodec.BufferInfo): ByteBuffer {
        val dataLen = mDataBuffer.size
        val packetStart = mPacketStart[index]
        val length = mPacketLength[index]
        info.flags = mPacketFlags[index]
        info.offset = packetStart
        info.presentationTimeUs = mPacketPtsUsec[index]
        info.size = length
        if (packetStart + length <= dataLen) {
            // one chunk; return full buffer to avoid copying data
            return mDataBufferWrapper
        } else {
            // two chunks
            val tempBuf = ByteBuffer.allocateDirect(length)
            val firstSize = dataLen - packetStart
            tempBuf.put(mDataBuffer, mPacketStart[index], firstSize)
            tempBuf.put(mDataBuffer, 0, length - firstSize)
            info.offset = 0
            return tempBuf
        }
    }// list is empty

    /**
     * Computes the data buffer offset for the next place to store data.
     *
     *
     * Equal to the start of the previous packet's data plus the previous packet's length.
     */
    private val headStart: Int
        get() {
            if (mMetaHead == mMetaTail) {
                // list is empty
                return 0
            }
            val dataLen = mDataBuffer.size
            val metaLen = mPacketStart.size
            val beforeHead = (mMetaHead + metaLen - 1) % metaLen
            return (mPacketStart[beforeHead] + mPacketLength[beforeHead] + 1) % dataLen
        }

    /**
     * Determines whether this is enough space to fit "size" bytes in the data buffer, and
     * one more packet in the meta-data buffer.
     *
     * @return True if there is enough space to add without removing anything.
     */
    private fun canAdd(size: Int): Boolean {
        val dataLen = mDataBuffer.size
        val metaLen = mPacketStart.size
        if (size > dataLen) {
            throw RuntimeException(
                "Enormous packet: " + size + " vs. buffer " +
                        dataLen
            )
        }
        if (mMetaHead == mMetaTail) {
            // empty list
            return true
        }

        // Make sure we can advance head without stepping on the tail.
        val nextHead = (mMetaHead + 1) % metaLen
        if (nextHead == mMetaTail) {
            if (VERBOSE) {
                Log.v(
                    TAG,
                    "ran out of metadata (head=$mMetaHead tail=$mMetaTail)"
                )
            }
            return false
        }

        // Need the byte offset of the start of the "tail" packet, and the byte offset where
        // "head" will store its data.
        val headStart = headStart
        val tailStart = mPacketStart[mMetaTail]
        val freeSpace = (tailStart + dataLen - headStart) % dataLen
        if (size > freeSpace) {
            if (VERBOSE) {
                Log.v(
                    TAG, ("ran out of data (tailStart=" + tailStart + " headStart=" + headStart +
                            " req=" + size + " free=" + freeSpace + ")")
                )
            }
            return false
        }
        if (VERBOSE) {
            Log.v(
                TAG, ("OK: size=" + size + " free=" + freeSpace + " metaFree=" +
                        ((mMetaTail + metaLen - mMetaHead) % metaLen - 1))
            )
        }
        return true
    }

    /**
     * Removes the tail packet.
     */
    private fun removeTail() {
        if (mMetaHead == mMetaTail) {
            throw RuntimeException("Can't removeTail() in empty buffer")
        }
        val metaLen = mPacketStart.size
        mMetaTail = (mMetaTail + 1) % metaLen
    }

    companion object {
        private const val TAG: String = "CircularEncoderBufTAG"
        private val EXTRA_DEBUG = true
        private val VERBOSE = false
    }

    /**
     * Allocates the circular buffers we use for encoded data and meta-data.
     */
    init {
        // For the encoded data, we assume the encoded bit rate is close to what we request.
        //
        // There would be a minor performance advantage to using a power of two here, because
        // not all ARM CPUs support integer modulus.
        val dataBufferSize = bitRate * desiredSpanSec / 8
        mDataBuffer = ByteArray(dataBufferSize)
        mDataBufferWrapper = ByteBuffer.wrap(mDataBuffer)

        // Meta-data is smaller than encoded data for non-trivial frames, so we over-allocate
        // a bit.  This should ensure that we drop packets because we ran out of (expensive)
        // data storage rather than (inexpensive) metadata storage.
        val metaBufferCount = frameRate * desiredSpanSec * 2
        mPacketFlags = IntArray(metaBufferCount)
        mPacketPtsUsec = LongArray(metaBufferCount)
        mPacketStart = IntArray(metaBufferCount)
        mPacketLength = IntArray(metaBufferCount)
        if (VERBOSE) {
            Log.d(
                TAG, ("CBE: bitRate=" + bitRate + " frameRate=" + frameRate +
                        " desiredSpan=" + desiredSpanSec + ": dataBufferSize=" + dataBufferSize +
                        " metaBufferCount=" + metaBufferCount)
            )
        }
    }
}