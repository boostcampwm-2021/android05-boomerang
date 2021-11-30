package com.kotlinisgood.boomerang.ui.home

import android.content.res.Resources
import android.database.CursorIndexOutOfBoundsException
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.databinding.ItemRvHomeShowAudioBinding
import com.kotlinisgood.boomerang.databinding.ItemRvHomeShowVideosBinding
import com.kotlinisgood.boomerang.util.*
import java.lang.Exception
import java.lang.NullPointerException


class HomeAdapter(private val homeViewModel: HomeViewModel) :
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
                try {
                    if (currentList[position].memoHeight == DEFAULT_HEIGHT_WIDTH && currentList[position].memoWidth == DEFAULT_HEIGHT_WIDTH) {
                        val mmr = MediaMetadataRetriever()
                        val uri = currentList[position].mediaUri

                        if (uri.startsWith("content")) {
                            mmr.setDataSource(
                                UriUtil.getPathFromUri(
                                    holder.itemView.context.contentResolver,
                                    currentList[position].mediaUri.toUri()
                                )
                            )
                        } else {
                            mmr.setDataSource(uri)
                        }
                        val height =
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                                ?.toInt()!!
                        val width =
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                                ?.toInt()!!
                        currentList[position].memoHeight = height
                        currentList[position].memoWidth = width
                        homeViewModel.updateMediaMemo(currentList[position])
                    }
                    val item = currentList[position]
                    val lp = holder.binding.itemIvHomeVideoThumbnail.layoutParams

                    val ratio = item.memoHeight / item.memoWidth
                    lp.height = lp.width * ratio
                    holder.binding.itemIvHomeVideoThumbnail.layoutParams = lp
                    Glide.with(holder.itemView.context).load(item.mediaUri)
                        .into(holder.binding.itemIvHomeVideoThumbnail)
                    holder.bind(getItem(position))
                } catch (e: Exception) {
                    when (e) {
                        is CursorIndexOutOfBoundsException -> {
                            e.printStackTrace()
                            homeViewModel.deleteMediaMemo(currentList[position])
                            submitList(currentList.toMutableList().apply { removeAt(position) })
                        }
                        is NullPointerException -> {
                            e.printStackTrace()
                            homeViewModel.deleteMediaMemo(currentList[position].id)
                            submitList(currentList.toMutableList().apply { removeAt(position) })
                        }
                    }
                }
            }
            is AudioMemoViewHolder -> holder.bind(getItem(position))
        }
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
                itemClickListener?.onItemClick(itemView, bindingAdapterPosition)
            }
        }

        fun bind(item: MediaMemo) {
            binding.mediaMemo = item
            if (item.memoType != AUDIO_MODE) {
                binding.itemIvHomeVideoThumbnail.imageFromVideoMemo(item)
                when (item.memoType) {
                    VIDEO_MODE_SUB_VIDEO -> {
                        binding.ivIcon.setBackgroundResource(R.drawable.ic_person)
                        binding.ivIcon.contentDescription = Resources.getSystem()
                            .getString(R.string.item_rv_home_show_videos_icon_cd_person)
                    }
                    VIDEO_MODE_FRAME -> {
                        binding.ivIcon.setBackgroundResource(R.drawable.ic_people)
                        binding.ivIcon.contentDescription = Resources.getSystem()
                            .getString(R.string.item_rv_home_show_videos_icon_cd_people)
                    }
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
                itemClickListener?.onItemClick(itemView, bindingAdapterPosition)
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