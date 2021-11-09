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

//    private val queryChannel = BroadcastChannel<String>(Channel.CONFLATED)

    private var _videoMemo = MutableLiveData<List<VideoMemo>>()
    val videoMemo: LiveData<List<VideoMemo>> = _videoMemo

    private var _orderSetting = MutableLiveData<OrderState>()
    val orderSetting: LiveData<OrderState> get() = _orderSetting

    init {
        getOrderState()
//        setSearchResult()
    }

   /* @FlowPreview
    @ExperimentalCoroutinesApi
    fun setSearchResult() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                queryChannel
                    .asFlow()
                    .debounce(1000)
                    .filter {
                        return@filter it.isNotEmpty()
                    }
                    .distinctUntilChanged()
                    .mapLatest { query ->
                        try {
                            repository.searchVideoByKeyword(query)
                        } catch (e: CancellationException) {
                            throw e
                        }
                    }
            }.collect {
                _videoMemo.value = it
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun sendQueryToChannel(query: String?) {
        query?: return
        viewModelScope.launch {
            queryChannel.send(query)
        }
    }*/

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

    private fun changeVideosOrder() {
        val tmpVideos = videoMemo.value?.toMutableList() ?: return
        val order = orderSetting.value?.order ?: return
        _videoMemo.value =  if (order == "modify") {
            tmpVideos.sortedBy { it.editTime }.reversed()
        } else {
            tmpVideos.sortedBy { it.createTime }.reversed()
        }
    }

    private fun getOrderState() {
        _orderSetting.value = sharedPref.getOrderState()
    }

    fun setOrderState(orderState: OrderState) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sharedPref.saveOrderState(orderState)
            }
            _orderSetting.value = orderState
            changeVideosOrder()
        }
    }

    fun searchVideos(query: String?) {
        query?: return
        viewModelScope.launch {
            val videos = withContext(Dispatchers.IO) {
                repository.searchVideoByKeyword(query).reversed()
            }
            _videoMemo.value = videos
        }
    }

}