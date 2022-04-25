package com.kotlinisgood.boomerang.ui.audiorecord

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import com.kotlinisgood.boomerang.util.AUDIO_MODE
import com.kotlinisgood.boomerang.util.DEFAULT_HEIGHT_WIDTH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zeroonezero.android.audio_mixer.AudioMixer
import zeroonezero.android.audio_mixer.input.GeneralAudioInput
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioRecordViewModel
@Inject constructor(private val repository: AppRepository) : ViewModel() {

    private var _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _audioMemo = MutableLiveData<MediaMemo>()
    val audioMemo: LiveData<MediaMemo> get() = _audioMemo

    private val fileList = mutableListOf<File>()
    private val timeList = mutableListOf(0)
    private val textList = mutableListOf<String>()

    fun saveAudioMemo(title: String, baseFile: File) {
        val createTime = System.currentTimeMillis()
        val outputPath = baseFile.absolutePath + "/$createTime.mp4"
        try {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    mixAudioFilesToOneFile(title, outputPath, createTime)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _loading.postValue(false)
        }
    }

    private fun mixAudioFilesToOneFile(title: String, outputPath: String, createTime: Long) {
        AudioMixer(outputPath).apply {
            prepareFilesForMix(this)
            mixingType = AudioMixer.MixingType.SEQUENTIAL
            setProcessingListener(object : AudioMixer.ProcessingListener {

                override fun onProgress(progress: Double) {
                    _loading.postValue(true)
                }

                override fun onEnd() {
                    _loading.postValue(false)
                    _audioMemo.postValue(
                        MediaMemo(
                            title, outputPath, createTime, createTime,
                            AUDIO_MODE, emptyList(), textList, timeList,
                            DEFAULT_HEIGHT_WIDTH, DEFAULT_HEIGHT_WIDTH
                        ).also {
                            viewModelScope.launch {
                                repository.saveMediaMemo(it)
                                deleteAudios()
                            }
                        })
                }
            })
        }.also {
            it.start()
            it.processAsync()
        }
    }

    private fun prepareFilesForMix(audioMixer: AudioMixer) {
        fileList.forEach {
            audioMixer.addDataSource(GeneralAudioInput(it.absolutePath))
        }
    }

    fun deleteAudios() {
        fileList.forEach {
            it.delete()
        }
        fileList.clear()
        textList.clear()
        timeList.clear()
        timeList.add(0)
    }

    fun addTimeAndText(recognizedText: String, duration: Int) {
        timeList.add(timeList.last() + duration)
        textList.add(recognizedText)
    }

    fun addFileToList(file: File) {
        fileList.add(file)
    }

    fun getAccText(): String {
        return textList.reduce { acc, str -> "$acc\n$str" }
    }

    fun isFileListEmpty() = (fileList.size == 0)

}