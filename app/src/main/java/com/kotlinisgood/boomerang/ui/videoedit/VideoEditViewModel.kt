package com.kotlinisgood.boomerang.ui.videoedit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.VideoMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoEditViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    private var memoTitle = ""
    private var videoUri: Uri = Uri.EMPTY
    private var subVideos = mutableListOf<SubVideo>()

    fun saveMemo() {
        println(subVideos)
        val memo: VideoMemo = if (subVideos.size == 0) {
            VideoMemo(
                memoTitle,
                videoUri.toString(),
                subVideos,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                VideoEditFragment.VIDEO_MODE_FRAME
            )
        } else {
            VideoMemo(
                memoTitle,
                videoUri.toString(),
                subVideos,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                VideoEditFragment.VIDEO_MODE_SUB_VIDEO
            )
        }
        viewModelScope.launch {
            repository.saveVideoMemo(memo)
        }
    }

    fun setTitle(memoTitle: String) {
        this.memoTitle = memoTitle
    }

    fun getTitle(): String {
        return memoTitle
    }

    fun setVideoUri(uri: Uri) {
        videoUri = uri
    }

    fun getVideoUri(): Uri {
        return videoUri
    }

    fun setSubVideo(subVideos: MutableList<SubVideo>) {
        this.subVideos = subVideos
    }

    fun getSubVideo(): MutableList<SubVideo> {
        return subVideos
    }
}