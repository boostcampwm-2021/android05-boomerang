package com.kotlinisgood.boomerang.ui.videoeditlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.VideoMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoEditViewModel @Inject constructor(
    private val repository : AppRepository,
) : ViewModel(){
    fun saveMemo(memo: VideoMemo){
        viewModelScope.launch{
            repository.saveVideoMemo(memo)
        }
    }
}