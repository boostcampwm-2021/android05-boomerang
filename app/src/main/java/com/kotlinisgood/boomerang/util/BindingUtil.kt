package com.kotlinisgood.boomerang.util

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.TextView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import java.text.SimpleDateFormat

@Suppress("UNCHECKED_CAST")
@BindingAdapter("submitList")
fun <T, VH : RecyclerView.ViewHolder> RecyclerView.submitList(list: List<T>?) {
    println("=======List: $list")
    list?.let {
        (adapter as ListAdapter<T, VH>).submitList(list)
    }
}

@BindingAdapter("imageFromVideoMemo")
fun ShapeableImageView.imageFromVideoMemo(mediaMemo: MediaMemo) {
    val uri = Uri.parse(mediaMemo.mediaUri)
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(context, uri)
    val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val durationInMillisec = duration!!.toLong()
    val bmp = mmr.getFrameAtTime(durationInMillisec * 1000 / 2)
    setImageBitmap(bmp)
}

@SuppressLint("SetTextI18n", "SimpleDateFormat")
@BindingAdapter("dateFromVideoMemo")
fun TextView.dateFromVideoMemo(mediaMemo: MediaMemo) {
    val sdf = SimpleDateFormat("yyyy-MM-dd")
    val createTimeStr = sdf.format(mediaMemo.createTime)
    val editTimeStr = sdf.format(mediaMemo.modifyTime)
    text = "생성일: $createTimeStr\n수정일: $editTimeStr"
}

@BindingAdapter("imageFromSubVideo")
fun ShapeableImageView.imageFromSubVideo(subVideo: SubVideo){
    val uri = subVideo.uri.toUri()
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(context, uri)
    val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val durationInMillisec = duration!!.toLong()
    val bmp = mmr.getFrameAtTime(durationInMillisec * 1000)
    setImageBitmap(bmp)
}

@SuppressLint("SetTextI18n")
@BindingAdapter("timeFromSubVideo")
fun TextView.timeFromSubVideo(subVideo: SubVideo){
    val startMinute = subVideo.startingTime/1000 / 60 % 60
    val startSecond = subVideo.startingTime/1000 % 60
    val endMinute = subVideo.endingTime/1000 / 60 % 60
    val endSecond = subVideo.endingTime/1000 % 60
    val startingTime = String.format("%02d:%02d", startMinute, startSecond)
    val endingTime = String.format("%02d:%02d", endMinute,endSecond)
    text = "$startingTime ~ $endingTime"
}
