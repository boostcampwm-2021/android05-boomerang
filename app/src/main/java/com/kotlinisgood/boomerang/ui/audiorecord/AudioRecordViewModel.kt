package com.kotlinisgood.boomerang.ui.audiorecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.util.AUDIO_MODE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioRecordViewModel
@Inject constructor(private val repository: AppRepository) : ViewModel() {

    private val audioList = mutableListOf<MediaMemo>()
    private var _currentAudio: MediaMemo? = null
    val currentAudio get() = _currentAudio

    fun setCurrentAudio(
        title: String,
        path: String,
        createTime: Long,
        textList: List<String>,
        timeList: List<Int>
    ) {
        val tmpAudio = _currentAudio
        _currentAudio = MediaMemo(
            title,
            path,
            createTime,
            createTime,
            AUDIO_MODE,
            emptyList<SubVideo>(),
            textList,
            timeList
        )
        if (tmpAudio != _currentAudio) {
            tmpAudio?.let { audioList.add(it) }
        }
        println(currentAudio)
    }

    fun saveAudioMemo(title: String) {
        _currentAudio = copyCurrentAudio(title)
        currentAudio?.let {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.saveMediaMemo(it)
                }
                deleteAudios()
            }
        }
    }

    private fun deleteAudios() {
        audioList.forEach {
            val file = File(it.mediaUri)
            file.delete()
        }
        audioList.clear()
    }

    private fun copyCurrentAudio(title: String): MediaMemo? {
        val tmpAudio = _currentAudio ?: return null
        return MediaMemo(
            title,
            tmpAudio.mediaUri,
            tmpAudio.createTime,
            tmpAudio.createTime,
            AUDIO_MODE,
            emptyList<SubVideo>(),
            tmpAudio.textList,
            tmpAudio.timeList
        )
    }

}