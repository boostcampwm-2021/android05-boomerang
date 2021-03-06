package com.kotlinisgood.boomerang.ui.videosave

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphamovie.lib.AlphaMovieView
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.util.VIDEO_MODE_FRAME
import com.kotlinisgood.boomerang.util.VIDEO_MODE_SUB_VIDEO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoSaveViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    private var memoType = false
    private var memoTitle = ""

    private var videoUri: Uri = Uri.EMPTY
    private var subVideos = mutableListOf<SubVideo>()

    var alphaMovieViews: MutableList<AlphaMovieView> = mutableListOf()
    private var subVideosStates = mutableListOf<Boolean>()

    fun saveMemo(height: Int, width: Int) {
        val memo = MediaMemo(
            memoTitle,
            videoUri.toString(),
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            VIDEO_MODE_FRAME,
            subVideos,
            emptyList(),
            emptyList(),
            height,
            width
        )
        viewModelScope.launch {
            repository.saveMediaMemo(memo)
        }
    }

    fun saveMemo(uri: String, height: Int, width: Int) {
        val memo = MediaMemo(
            memoTitle,
            uri,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            VIDEO_MODE_SUB_VIDEO,
            subVideos,
            emptyList(),
            emptyList(),
            height,
            width
        )
        viewModelScope.launch {
            repository.saveMediaMemo(memo)
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
        repeat(subVideos.size) { subVideosStates.add(false) }
    }

    fun getSubVideo(): MutableList<SubVideo> {
        return subVideos
    }

    fun getSubVideoStates(): MutableList<Boolean> {
        return subVideosStates
    }

    fun addAlphaMovieView(alphaMovieView: AlphaMovieView) {
        alphaMovieViews.add(alphaMovieView)
    }

    fun setMemoType(memoType: Boolean) {
        this.memoType = memoType
    }
}