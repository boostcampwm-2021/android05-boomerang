package com.kotlinisgood.boomerang.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.databinding.ItemRvHomeShowVideosBinding

class HomeAdapter :
    ListAdapter<MediaMemo, HomeAdapter.MemoViewHolder>(MediaMemoDiffCallback()) {

    private var itemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        return MemoViewHolder(
            ItemRvHomeShowVideosBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemoViewHolder(
        private val binding: ItemRvHomeShowVideosBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                itemClickListener?.onItemClick(itemView, adapterPosition)
            }
        }

        fun bind(item: MediaMemo) {
            binding.mediaMemo = item
//            when (item.memoType) {
//                VIDEO_MODE_FRAME -> binding.itemLayoutHomeVideo.setBackgroundColor(Color.CYAN)
//                VIDEO_MODE_SUB_VIDEO -> binding.itemLayoutHomeVideo.setBackgroundColor(Color.MAGENTA)
//            }
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