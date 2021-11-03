package com.kotlinisgood.boomerang.ui.videoselection

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kotlinisgood.boomerang.databinding.ItemRvVideoSelectionShowVideosBinding
import com.kotlinisgood.boomerang.ui.home.ExternalVideoDTO
import com.kotlinisgood.boomerang.ui.home.ExternalVideoDiffItemCallback

class VideoSelectionAdapter(
    private val contentResolver: ContentResolver
) : ListAdapter<ExternalVideoDTO, VideoSelectionAdapter.VideoSelectionViewHolder>(
    ExternalVideoDiffItemCallback
) {

    var selectedIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoSelectionViewHolder {
        return VideoSelectionViewHolder(
            ItemRvVideoSelectionShowVideosBinding
                .inflate(LayoutInflater.from(parent.context), parent, false),
            contentResolver
        )
    }

    override fun onBindViewHolder(holder: VideoSelectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoSelectionViewHolder(
        private val binding: ItemRvVideoSelectionShowVideosBinding,
        private val contentResolver: ContentResolver
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (selectedIndex != -1) {
                    currentList[selectedIndex].isChecked = false
                }
                notifyItemChanged(selectedIndex)
                adapterPosition.also {
                    currentList[it].isChecked = true
                    selectedIndex = it
                    notifyItemChanged(it)
                }
            }
        }

        fun bind(item: ExternalVideoDTO) {
            val thumbnail = MediaMetadataRetriever().run {
                // ToDo (Writer: Green) getPathFromUri 가 "" 일 경우에, 기본 이미지 지정 후 보여줄 것
                this.setDataSource(getPathFromUri(item.uri))
                getFrameAtTime(0L)
            }
            binding.itemIvVideoSelectionThumbnail.setImageBitmap(thumbnail)
            binding.itemCbVideoSelection.isChecked = item.isChecked
        }

        @SuppressLint("Range")
        fun getPathFromUri(uri: Uri): String {
            val cursor = contentResolver.query(uri, null, null, null, null) ?: return ""
            cursor.moveToNext()
            val path = cursor.getString(cursor.getColumnIndex("_data"))
            cursor.close()
            return path
        }
    }

}