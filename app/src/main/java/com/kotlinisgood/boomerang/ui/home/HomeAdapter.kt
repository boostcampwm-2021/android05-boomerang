package com.kotlinisgood.boomerang.ui.home

import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.databinding.ItemRvHomeShowAudioBinding
import com.kotlinisgood.boomerang.databinding.ItemRvHomeShowVideosBinding
import com.kotlinisgood.boomerang.util.*


class HomeAdapter (private val homeViewModel: HomeViewModel) :
    ListAdapter<MediaMemo, RecyclerView.ViewHolder>(MediaMemoDiffCallback()) {

    private var itemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            AUDIO_MODE -> {
                AudioMemoViewHolder(
                    ItemRvHomeShowAudioBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    )
                )
            }
            else -> {
                VideoMemoViewHolder(
                    ItemRvHomeShowVideosBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VideoMemoViewHolder -> {
                if (currentList[position].memoHeight == DEFAULT_HEIGHT_WIDTH && currentList[position].memoWidth == DEFAULT_HEIGHT_WIDTH) {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(currentList[position].mediaUri)
                    val height =
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ?.toInt()!!
                    val width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?.toInt()!!
                    currentList[position].memoHeight = height
                    currentList[position].memoWidth = width
                    homeViewModel.updateMediaMemo(currentList[position])
                }
                val vh = holder
                val item = currentList.get(position)
                val lp = vh.binding.itemIvHomeVideoThumbnail.layoutParams

                val ratio = item.memoHeight / item.memoWidth
                lp.height = lp.width * ratio
                vh.binding.itemIvHomeVideoThumbnail.layoutParams = lp
                Glide.with(vh.itemView.context).load(item.mediaUri)
                    .into(vh.binding.itemIvHomeVideoThumbnail)
                holder.bind(getItem(position))
            }
            is AudioMemoViewHolder -> holder.bind(getItem(position))
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        val lp = (holder as VideoMemoViewHolder).binding.itemIvHomeVideoThumbnail.layoutParams
        lp.height = 100
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).memoType
    }

    inner class VideoMemoViewHolder(
        val binding: ItemRvHomeShowVideosBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.orderState = homeViewModel.orderSetting.value
            itemView.setOnClickListener {
                itemClickListener?.onItemClick(itemView, adapterPosition)
            }
        }

        fun bind(item: MediaMemo) {
            binding.mediaMemo = item
            if (item.memoType != AUDIO_MODE) {
                binding.itemIvHomeVideoThumbnail.imageFromVideoMemo(item)
                when (item.memoType) {
                    VIDEO_MODE_SUB_VIDEO -> binding.ivIcon.setBackgroundResource(R.drawable.ic_person)
                    VIDEO_MODE_FRAME -> binding.ivIcon.setBackgroundResource(R.drawable.ic_people)
                }
            }
        }
    }

    inner class AudioMemoViewHolder(
        private val binding: ItemRvHomeShowAudioBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.orderState = homeViewModel.orderSetting.value
            itemView.setOnClickListener {
                itemClickListener?.onItemClick(itemView, adapterPosition)
            }
        }

        fun bind(item: MediaMemo) {
            binding.mediaMemo = item
        }
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }
}


class MediaMemoDiffCallback : DiffUtil.ItemCallback<MediaMemo>() {
    override fun areItemsTheSame(oldItem: MediaMemo, newItem: MediaMemo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MediaMemo, newItem: MediaMemo): Boolean {
        return oldItem == newItem
    }
}