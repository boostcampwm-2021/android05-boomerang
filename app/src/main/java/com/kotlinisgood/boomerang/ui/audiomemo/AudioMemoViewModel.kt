package com.kotlinisgood.boomerang.ui.audiomemo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AudioMemoViewModel @Inject constructor(val repository: AppRepository): ViewModel() {

    private var _mediaMemo = MutableLiveData<MediaMemo>()
    val mediaMemo: LiveData<MediaMemo> get() = _mediaMemo
    private val _timeSeriesTextList = MutableLiveData<List<TimeSeriesText>>(emptyList())
    val timeSeriesTextList: LiveData<List<TimeSeriesText>> get() = _timeSeriesTextList

    fun getMediaMemo(id: Int) {
        viewModelScope.launch {
            _mediaMemo.value = withContext(Dispatchers.IO) {
                repository.getMediaMemo(id)
            }
            _mediaMemo.value?.let {
                _timeSeriesTextList.value = it.textList.mapIndexed { idx, text ->
                    TimeSeriesText(idx, it.timeList[idx], text)
                }.toList()
            }
            println("mediaMeo :  ${_mediaMemo.value}")
            println("timeSeriesTextList :  ${_timeSeriesTextList.value}")
        }
    }

}