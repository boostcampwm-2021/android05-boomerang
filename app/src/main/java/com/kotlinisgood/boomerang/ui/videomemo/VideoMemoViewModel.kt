package com.kotlinisgood.boomerang.ui.videomemo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphamovie.lib.AlphaMovieView
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.util.VIDEO_MODE_FRAME
import com.kotlinisgood.boomerang.util.VIDEO_MODE_SUB_VIDEO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoMemoViewModel @Inject constructor(private val repository: AppRepository) : ViewModel() {

    private var _isLoading = MutableLiveData(false)
    val isLoading : LiveData<Boolean> get() = _isLoading

    private var _mediaMemo: MutableLiveData<MediaMemo> = MutableLiveData()
    val mediaMemo: LiveData<MediaMemo> get() = _mediaMemo
    private var subVideos = listOf<SubVideo>()
    var alphaMovieViews: MutableList<AlphaMovieView> = mutableListOf()
    private var subVideosStates = mutableListOf<Boolean>()

    fun loadMediaMemo(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _mediaMemo.value = repository.getMediaMemo(id)
            subVideos = mediaMemo.value?.memoList!!
            repeat(subVideos.size) { subVideosStates.add(false) }
            _isLoading.value = false
        }
    }

    fun getSubVideo(): List<SubVideo> {
        return subVideos
    }

    fun getSubVideoStates(): MutableList<Boolean> {
        return subVideosStates
    }


    fun addAlphaMovieView(alphaMovieView: AlphaMovieView) {
        alphaMovieViews.add(alphaMovieView)
    }

    suspend fun deleteMemo(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                mediaMemo.value?.let {
                    repository.deleteMemo(it)
                    if (it.memoType == VIDEO_MODE_FRAME) {
                        File(it.mediaUri).delete()
                    } else if (it.memoType == VIDEO_MODE_SUB_VIDEO) {
                        it.memoList.forEach { subVideo ->
                            val file = File(subVideo.uri)
                            File(subVideo.uri).delete()
                        }
                    }
                    true
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

}