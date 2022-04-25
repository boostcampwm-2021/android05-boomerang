package com.kotlinisgood.boomerang.ui.videoselection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kotlinisgood.boomerang.databinding.ItemRvVideoSelectionShowVideosBinding

class VideoSelectionAdapter : ListAdapter<ExternalVideoDTO, VideoSelectionAdapter.VideoSelectionViewHolder>(
    ExternalVideoDiffItemCallback()
) {

    var selectedIndex = -1

    fun setSelectionComplete() {
        currentList[selectedIndex].isChecked = false
        notifyItemChanged(selectedIndex)
        selectedIndex = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoSelectionViewHolder {
        return VideoSelectionViewHolder(
            ItemRvVideoSelectionShowVideosBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: VideoSelectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoSelectionViewHolder(
        private val binding: ItemRvVideoSelectionShowVideosBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (selectedIndex != -1) {
                    currentList[selectedIndex].isChecked = false
                    notifyItemChanged(selectedIndex)
                }
                if (selectedIndex != -1 && selectedIndex == absoluteAdapterPosition) {
                    selectedIndex = -1
                } else {
                    selectedIndex = absoluteAdapterPosition
                    currentList[selectedIndex].isChecked = true
                    notifyItemChanged(selectedIndex)
                }
            }
        }

        fun bind(item: ExternalVideoDTO) {
            binding.itemCbVideoSelection.isChecked = item.isChecked
            Glide.with(binding.root)
                .load(item.uri)
                .into(binding.itemIvVideoSelectionThumbnail)
        }
    }

}

class ExternalVideoDiffItemCallback : DiffUtil.ItemCallback<ExternalVideoDTO>() {
    override fun areItemsTheSame(oldItem: ExternalVideoDTO, newItem: ExternalVideoDTO): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: ExternalVideoDTO, newItem: ExternalVideoDTO): Boolean {
        return oldItem == newItem
    }
}
