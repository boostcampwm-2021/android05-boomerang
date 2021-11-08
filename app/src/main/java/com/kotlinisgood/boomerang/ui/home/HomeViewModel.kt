package com.kotlinisgood.boomerang.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.VideoMemo
import com.kotlinisgood.boomerang.model.OrderState
import com.kotlinisgood.boomerang.repository.AppRepository
import com.kotlinisgood.boomerang.repository.SharedPrefDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val sharedPref: SharedPrefDataSource,
) : ViewModel() {

    private var _videoMemo = MutableLiveData<List<VideoMemo>>()
    val videoMemo: LiveData<List<VideoMemo>> = _videoMemo

    private var _orderSetting = MutableLiveData<OrderState>()
    val orderSetting: LiveData<OrderState> get() = _orderSetting

    init {
        getOrderState()
    }

    fun loadVideoMemo() {
        viewModelScope.launch {
            val videos = repository.getVideoMemos()
            _videoMemo.value = if (orderSetting.value == OrderState.MODIFY) {
                videos.sortedBy { it.editTime }.reversed()
            } else {
                videos.sortedBy { it.createTime }.reversed()
            }
        }
    }

    fun getOrderState() {
        _orderSetting.value = sharedPref.getOrderState()
    }

    fun setOrderState(orderState: OrderState) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sharedPref.saveOrderState(orderState)
                _orderSetting.value = orderState
            }
        }
    }

}