package com.kotlinisgood.boomerang.ui.home

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kotlinisgood.boomerang.databinding.ItemRvHomeShowVideosBinding
import com.kotlinisgood.boomerang.util.StringUtil

class MemoListAdapter(
    private val contentResolver: ContentResolver
) : ListAdapter<ExternalVideoDTO, MemoListAdapter.MemoViewHolder>(ExternalVideoDiffItemCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        return MemoViewHolder.from(parent,contentResolver)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MemoViewHolder(
        private val binding: ItemRvHomeShowVideosBinding,
        private val contentResolver: ContentResolver
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExternalVideoDTO) {
            binding.itemTvHomeVideoTitle.text = item.title
            binding.itemTvHomeVideoPlaytime.text = StringUtil.convertMilliSec(item.duration)
            binding.itemTvHomeVideoDayInfo.text = "1일 전"
            val thumbnail2 = MediaMetadataRetriever().run {
                // ToDo (Writer: Green) getPathFromUri 가 "" 일 경우에, 기본 이미지 지정 후 보여줄 것
                this.setDataSource(getPathFromUri(item.uri))
                getFrameAtTime(0L)
            }
            binding.itemIvHomeVideoThumbnail.setImageBitmap(thumbnail2)
        }

        @SuppressLint("Range")
        fun getPathFromUri(uri: Uri): String{
            val cursor = contentResolver.query(uri, null, null, null, null)?: return ""
            cursor.moveToNext()
            val path = cursor.getString(cursor.getColumnIndex("_data"))
            cursor.close()
            return path
        }

        companion object {
            fun from(parent: ViewGroup, contentResolver: ContentResolver): MemoViewHolder {
                return MemoViewHolder(
                    ItemRvHomeShowVideosBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    ),
                    contentResolver
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