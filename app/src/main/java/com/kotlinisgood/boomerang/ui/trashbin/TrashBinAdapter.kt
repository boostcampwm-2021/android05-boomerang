package com.kotlinisgood.boomerang.ui.trashbin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kotlinisgood.boomerang.database.entity.VideoMemo
import com.kotlinisgood.boomerang.databinding.ItemRvTrashBinShowVideosBinding

class TrashBinAdapter :
    ListAdapter<VideoMemo, TrashBinAdapter.TrashBinViewHolder>(VideoMemoDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashBinViewHolder {
        return TrashBinViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: TrashBinViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TrashBinViewHolder(
        private val binding: ItemRvTrashBinShowVideosBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VideoMemo) {
            binding.videoMemo = item
        }

        companion object {
            fun from(parent: ViewGroup): TrashBinViewHolder {
                return TrashBinViewHolder(
                    ItemRvTrashBinShowVideosBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    ),
                )
            }
        }
    }
}

object VideoMemoDiffCallback : DiffUtil.ItemCallback<VideoMemo>() {
    override fun areItemsTheSame(oldItem: VideoMemo, newItem: VideoMemo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: VideoMemo, newItem: VideoMemo): Boolean {
        return oldItem == newItem
    }
}