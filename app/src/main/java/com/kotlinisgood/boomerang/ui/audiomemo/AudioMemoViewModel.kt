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
import javax.inject.Inject

@HiltViewModel
class AudioMemoViewModel @Inject constructor(val repository: AppRepository): ViewModel() {

    private var _mediaMemo = MutableLiveData<MediaMemo>()
    val mediaMemo: LiveData<MediaMemo> get() = _mediaMemo
    val timeSeriesTextList = MutableLiveData<List<TimeSeriesText>>()

    fun getMediaMemo(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _mediaMemo.postValue(repository.getMediaMemo(id))
            _mediaMemo.value?.let {
                timeSeriesTextList.postValue(it.textList.mapIndexed { idx, text ->
                    TimeSeriesText(idx, it.timeList[idx], text)
                })
            }
        }
    }

}