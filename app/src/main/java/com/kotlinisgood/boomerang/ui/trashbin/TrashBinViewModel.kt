package com.kotlinisgood.boomerang.ui.trashbin

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
class TrashBinViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    private var _trashVideoMemo = MutableLiveData<List<VideoMemo>>()
    val trashVideoMemo: LiveData<List<VideoMemo>> = _trashVideoMemo

    fun loadTrashVideoMemo() {
        viewModelScope.launch {
            _trashVideoMemo.value = repository.getVideoMemos()
        }
    }
}