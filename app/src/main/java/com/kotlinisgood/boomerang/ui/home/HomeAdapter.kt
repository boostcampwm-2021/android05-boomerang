package com.kotlinisgood.boomerang.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.databinding.ItemRvHomeShowAudioBinding
import com.kotlinisgood.boomerang.databinding.ItemRvHomeShowVideosBinding
import com.kotlinisgood.boomerang.model.OrderState
import com.kotlinisgood.boomerang.util.AUDIO_MODE
import com.kotlinisgood.boomerang.util.imageFromVideoMemo


class HomeAdapter(private val liveData: LiveData<OrderState>) :
    ListAdapter<MediaMemo, HomeAdapter.MemoViewHolder>(MediaMemoDiffCallback()) {

    private var itemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
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
            is VideoMemoViewHolder -> holder.bind(getItem(position))
            is AudioMemoViewHolder -> holder.bind(getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).memoType
    }

    inner class VideoMemoViewHolder(
        private val binding: ItemRvHomeShowVideosBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.orderState = liveData.value
            itemView.setOnClickListener {
                itemClickListener?.onItemClick(itemView, adapterPosition)
            }
        }

        fun bind(item: MediaMemo) {
            binding.mediaMemo = item
            if (item.memoType != AUDIO_MODE) {
                binding.itemIvHomeVideoThumbnail.imageFromVideoMemo(item)
            }
        }
    }

    inner class AudioMemoViewHolder(
        private val binding: ItemRvHomeShowAudioBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
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