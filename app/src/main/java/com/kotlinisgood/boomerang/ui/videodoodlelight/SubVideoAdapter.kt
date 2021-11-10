package com.kotlinisgood.boomerang.ui.videodoodlelight

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kotlinisgood.boomerang.databinding.ItemRvDoodleLightSubvideosBinding
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideoAdapter.SubVideoViewHolder

class SubVideoAdapter : ListAdapter<SubVideo, SubVideoViewHolder>(SubVideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubVideoViewHolder {
        return SubVideoViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: SubVideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SubVideoViewHolder(
        private val binding: ItemRvDoodleLightSubvideosBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SubVideo) {
            binding.subVideo = item
        }

        companion object {
            fun from(parent: ViewGroup): SubVideoViewHolder {
                return SubVideoViewHolder(
                    ItemRvDoodleLightSubvideosBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    )
                )
            }
        }
    }

    override fun submitList(list: MutableList<SubVideo>?) {
        super.submitList(list)
    }

}

class SubVideoDiffCallback : DiffUtil.ItemCallback<SubVideo>() {
    override fun areItemsTheSame(oldItem: SubVideo, newItem: SubVideo): Boolean {
        return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: SubVideo, newItem: SubVideo): Boolean {
        return oldItem == newItem
    }
}