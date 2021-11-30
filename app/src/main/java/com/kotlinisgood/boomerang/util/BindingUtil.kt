package com.kotlinisgood.boomerang.util

import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import java.text.SimpleDateFormat
import java.util.*

@Suppress("UNCHECKED_CAST")
@BindingAdapter("submitList")
fun <T, VH : RecyclerView.ViewHolder> RecyclerView.submitList(list: List<T>?) {
    list?.let {
        (adapter as ListAdapter<T, VH>).submitList(list)
    }
}

@BindingAdapter("imageFromVideoMemo")
fun ImageView.imageFromVideoMemo(mediaMemo: MediaMemo) {
    val uri = Uri.parse(mediaMemo.mediaUri)
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(context, uri)
    val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: return
    val durationInMillisec = duration.toLong()
    val bmp = mmr.getFrameAtTime(durationInMillisec * 1000 / 2)
    setImageBitmap(bmp)
}

@BindingAdapter("showTextFromAudioMemo")
fun TextView.showTextFromAudioMemo(mediaMemo: MediaMemo) {
    if (mediaMemo.memoType != AUDIO_MODE) return
    val text = mediaMemo.textList[0]
    setText(text)
}


@BindingAdapter("setDurationFromMediaMemo")
fun TextView.setMemoTime(mediaMemo: MediaMemo) {
    val uri = Uri.parse(mediaMemo.mediaUri)
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(context, uri)
    val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: return
    val durationInMilli = duration.toLong()
    val minute = durationInMilli / 1000 / 60 % 60
    val second = durationInMilli / 1000 % 60
    val durationString = String.format("%02d:%02d", minute, second)
    text = durationString
}

@BindingAdapter("createDateFromMediaMemo")
fun TextView.createDateFromVideoMemo(mediaMemo: MediaMemo) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    val createTimeStr = sdf.format(mediaMemo.createTime)
    text = resources.getString(R.string.textview_create_date_from_video_memo, createTimeStr)
}

@BindingAdapter("editDateFromMediaMemo")
fun TextView.editDateFromMediaMemo(mediaMemo: MediaMemo) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    val editTimeStr = sdf.format(mediaMemo.modifyTime)
    text = resources.getString(R.string.textview_edit_date_from_video_memo, editTimeStr)
}

@BindingAdapter("setDateVisibility")
fun View.bindVisibility(visible: Boolean) {
    visibility = if (visible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}

@BindingAdapter("imageFromSubVideo")
fun ShapeableImageView.imageFromSubVideo(subVideo: SubVideo) {
    val uri = subVideo.uri.toUri()
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(context, uri)
    val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val durationInMillisec = duration!!.toLong()
    val bmp = mmr.getFrameAtTime(durationInMillisec * 1000)
    setImageBitmap(bmp)
}

@BindingAdapter("timeFromSubVideo")
fun TextView.timeFromSubVideo(subVideo: SubVideo) {
    val startMinute = subVideo.startingTime / 1000 / 60 % 60
    val startSecond = subVideo.startingTime / 1000 % 60
    val endMinute = subVideo.endingTime / 1000 / 60 % 60
    val endSecond = subVideo.endingTime / 1000 % 60
    val startingTime = String.format("%02d:%02d", startMinute, startSecond)
    val endingTime = String.format("%02d:%02d", endMinute, endSecond)
    text = resources.getString(R.string.textview_time_from_subVideo, startingTime, endingTime)
}

@BindingAdapter("isBold")
fun setBold(view: TextView, focused: Boolean) {
    if (focused) view.setTypeface(null, Typeface.BOLD) else view.setTypeface(null, Typeface.NORMAL)
}
