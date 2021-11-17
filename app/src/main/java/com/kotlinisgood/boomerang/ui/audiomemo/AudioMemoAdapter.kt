package com.kotlinisgood.boomerang.ui.audiomemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kotlinisgood.boomerang.databinding.ItemRvAudioMemoRecognizedTextBinding

class AudioMemoAdapter : ListAdapter<TimeSeriesText, AudioMemoAdapter.AudioMemoViewHolder>(TimeSeriesDiffItemCallback()) {

    private var audioMemoItemClickListener: OnAudioMemoItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioMemoViewHolder {
        return AudioMemoViewHolder(ItemRvAudioMemoRecognizedTextBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: AudioMemoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AudioMemoViewHolder(
        val binding: ItemRvAudioMemoRecognizedTextBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                audioMemoItemClickListener?.onItemClick(itemView, adapterPosition)
            }
        }

        fun bind(item: TimeSeriesText) {
            binding.timeSeriesText = item
        }
    }

    interface OnAudioMemoItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    fun setOnAudioMemoItemClickListener(listenerAudioMemo: OnAudioMemoItemClickListener) {
        audioMemoItemClickListener = listenerAudioMemo
    }
}

class TimeSeriesDiffItemCallback : DiffUtil.ItemCallback<TimeSeriesText>() {
    override fun areItemsTheSame(oldItem: TimeSeriesText, newItem: TimeSeriesText): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TimeSeriesText, newItem: TimeSeriesText): Boolean {
        return oldItem == newItem
    }

}