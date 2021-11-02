package com.kotlinisgood.boomerang.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kotlinisgood.boomerang.databinding.ItemRvHomeShowVideosBinding

class MemoListAdapter : ListAdapter<ExternalVideoDTO, MemoListAdapter.MemoViewHolder>(ExternalVideoDiffItemCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        return MemoViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    class MemoViewHolder(private val binding: ItemRvHomeShowVideosBinding)
    : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExternalVideoDTO) {
            binding.itemTvHomeVideoTitle.text = item.title
            binding.itemTvHomeVideoPlaytime.text = item.duration.toString()
            binding.itemTvHomeVideoDayInfo.text = "1일 전"
        }

        companion object {
            fun from(parent: ViewGroup): MemoViewHolder {
                return MemoViewHolder(
                    ItemRvHomeShowVideosBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    )
                )
            }
        }
    }
}

object ExternalVideoDiffItemCallback: DiffUtil.ItemCallback<ExternalVideoDTO>() {
    override fun areItemsTheSame(oldItem: ExternalVideoDTO, newItem: ExternalVideoDTO): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: ExternalVideoDTO, newItem: ExternalVideoDTO): Boolean {
        return oldItem.uri == newItem.uri
    }

}