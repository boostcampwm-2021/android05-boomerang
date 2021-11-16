package com.kotlinisgood.boomerang.ui.audiorecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.AudioMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioRecordViewModel
    @Inject constructor(private val repository: AppRepository) : ViewModel() {

    private val audioList = mutableListOf<AudioMemo>()
    private var _currentAudio: AudioMemo? = null
    val currentAudio get() = _currentAudio

    fun setCurrentAudio(
        title: String,
        path: String,
        createTime: Long,
        textList: List<String>,
        timeList: List<Int>
    ) {
        val tmpAudio = _currentAudio
        _currentAudio = AudioMemo(title, path, createTime, textList, timeList)
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
                    repository.saveAudioMemo(it)
                }
                deleteAudios()
            }
        }
    }

    private fun deleteAudios() {
        audioList.forEach {
            val file = File(it.path)
            file.delete()
        }
        audioList.clear()
    }

    private fun copyCurrentAudio(title: String): AudioMemo? {
        val tmpAudio = _currentAudio ?: return null
        return AudioMemo(
            title,
            tmpAudio.path,
            tmpAudio.createTime,
            tmpAudio.textList,
            tmpAudio.timeList
        )
    }

}