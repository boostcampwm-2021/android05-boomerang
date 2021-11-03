package com.kotlinisgood.boomerang.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.VideoMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    private var _videoMemo = MutableLiveData<List<VideoMemo>>()
    val videoMemo: LiveData<List<VideoMemo>> = _videoMemo

    fun loadVideoMemo() {
        viewModelScope.launch {
            _videoMemo.value = repository.getVideoMemos()
        }
    }
}