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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioMemoViewModel @Inject constructor(val repository: AppRepository): ViewModel() {

    private var _mediaMemo = MutableLiveData<MediaMemo>()
    val mediaMemo: LiveData<MediaMemo> get() = _mediaMemo
    private val _timeSeriesTextList = MutableLiveData<List<TimeSeriesText>>(emptyList())
    val timeSeriesTextList: LiveData<List<TimeSeriesText>> get() = _timeSeriesTextList
    private var _selected = -1

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
        }
    }

    private fun setTimeSeriesTextList(list: List<TimeSeriesText>) {
        _timeSeriesTextList.value = list
    }

    fun modifyFocusedTextOrNot(position: Long, list: List<TimeSeriesText>) {
        val newList = list.toMutableList()
        val item = newList.last { it.time <= position }
        val index = newList.indexOf(item)
        if (index != -1 && _selected != index) {
            if (_selected != -1) {
                newList[_selected] = newList[_selected].copy(focused = false)
            }
            setSelected(index)
            newList[index] = newList[index].copy(focused = true)
            setTimeSeriesTextList(newList.toList())
        }
    }

    private fun setSelected(index: Int) {
        _selected = index
    }

    suspend fun deleteMemo(): Boolean {
        return withContext(Dispatchers.IO) {
                try {
                    mediaMemo.value?.let {
                        repository.deleteMemo(it)
                        File(it.mediaUri).delete()
                        true
                    } ?: false
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
        }
    }

}