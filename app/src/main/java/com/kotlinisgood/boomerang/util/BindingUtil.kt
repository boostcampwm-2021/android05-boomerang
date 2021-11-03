package com.kotlinisgood.boomerang.util

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.kotlinisgood.boomerang.database.entity.VideoMemo
import java.text.SimpleDateFormat

@Suppress("UNCHECKED_CAST")
@BindingAdapter("submitList")
fun <T, VH : RecyclerView.ViewHolder> RecyclerView.submitList(list: List<T>?) {
    list?.let {
        (adapter as ListAdapter<T, VH>).submitList(list)
    }
}

@BindingAdapter("imageFromVideoMemo")
fun ShapeableImageView.imageFromVideoMemo(videoMemo: VideoMemo) {
    val uri = Uri.parse(videoMemo.videoUri)
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(context, uri)
    val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val durationInMillisec = duration!!.toLong()
    val bmp = mmr.getFrameAtTime(durationInMillisec * 1000 / 2)
    setImageBitmap(bmp)
}

@SuppressLint("SetTextI18n", "SimpleDateFormat")
@BindingAdapter("dateFromVideoMemo")
fun TextView.dateFromVideoMemo(videoMemo: VideoMemo) {
    val sdf = SimpleDateFormat("yyyy-MM-dd")
    val createTimeStr = sdf.format(videoMemo.createTime)
    val editTimeStr = sdf.format(videoMemo.editTime)
    text = "생성일: $createTimeStr\n수정일: $editTimeStr"
}
