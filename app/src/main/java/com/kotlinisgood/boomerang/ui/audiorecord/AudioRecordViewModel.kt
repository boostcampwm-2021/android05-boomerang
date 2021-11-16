package com.kotlinisgood.boomerang.ui.audiorecord

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import com.kotlinisgood.boomerang.ui.videodoodlelight.SubVideo
import com.kotlinisgood.boomerang.util.AUDIO_MODE
import dagger.hilt.android.lifecycle.HiltViewModel
import zeroonezero.android.audio_mixer.AudioMixer
import zeroonezero.android.audio_mixer.input.GeneralAudioInput
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioRecordViewModel
@Inject constructor(private val repository: AppRepository) : ViewModel() {

    private var _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading
    private val audioList = mutableListOf<MediaMemo>()
    private var _currentAudio: MediaMemo? = null
    val currentAudio get() = _currentAudio

    private val fileList = mutableListOf<File>()

    private val timeList = mutableListOf<Int>(0)
    private val textList = mutableListOf<String>()

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
    }

    fun saveAudioMemo(title: String, baseFile: File) {
//        _loading.value = true
//        _currentAudio = copyCurrentAudio(title)
//        currentAudio?.let {
//            viewModelScope.launch {
//                withContext(Dispatchers.IO) {
//                    repository.saveMediaMemo(it)
//                }
//                deleteAudios()
//                _loading.value = false
//            }
//        }

        val audioMixer = AudioMixer(baseFile.absolutePath + "/${System.currentTimeMillis()}.mp4").apply {
            fileList.forEach {
                addDataSource(GeneralAudioInput(it.absolutePath))
            }
            mixingType = AudioMixer.MixingType.SEQUENTIAL
            setProcessingListener(object: AudioMixer.ProcessingListener {
                override fun onProgress(progress: Double) {

                }
                override fun onEnd() {

                }

            })
        }

        audioMixer.also {
            it.start()
            it.processAsync()
        }
    }

    private fun deleteAudios() {
        audioList.forEach {
            val file = File(it.mediaUri)
            if (it != _currentAudio) {
                file.delete()
            }
        }
        audioList.clear()
        _currentAudio = null
    }

    private fun copyCurrentAudio(title: String): MediaMemo? {
        val tmpAudio = _currentAudio ?: return null
        return MediaMemo(
            title,
            tmpAudio.mediaUri,
            tmpAudio.createTime,
            tmpAudio.createTime,
            tmpAudio.memoType,
            emptyList<SubVideo>(),
            tmpAudio.textList,
            tmpAudio.timeList
        )
    }

    fun setTimeAndText(recognizedText: String, time: Int) {
        timeList.add(time)
        textList.add(recognizedText)
    }

    fun addFileList(file: File) {
        fileList.add(file)
    }

}