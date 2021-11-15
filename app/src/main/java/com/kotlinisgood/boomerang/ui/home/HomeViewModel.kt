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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val sharedPref: SharedPrefDataSource,
) : ViewModel() {

    //    private val queryChannel = BroadcastChannel<String>(Channel.CONFLATED)
    private val searchText: PublishSubject<String> = PublishSubject.create()

    //    private val kotlinDebounceText = debounce(500L, viewModelScope, ::searchVideos)
    private var _videoMemo = MutableLiveData<List<VideoMemo>>()
    val videoMemo: LiveData<List<VideoMemo>> = _videoMemo

    private var _orderSetting = MutableLiveData<OrderState>()
    private val orderSetting: LiveData<OrderState> get() = _orderSetting

    init {
        getOrderState()
        setQueryDebounceRxJava(searchText)
    }

    private fun setQueryDebounceRxJava(searchText: PublishSubject<String>) {
        searchText
            .debounce(500, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                searchVideos(it)
            }
            .subscribe()
    }

    fun sendQueryRxjava(query: String) {
        searchText.onNext(query)
    }

/*    fun sendQueryCoroutine(query: String) {
        kotlinDebounceText(query)
    }*/

/*
    private fun <T> debounce(
        waitMs: Long,
        scope: CoroutineScope,
        destinationFunction: (T) -> Unit
    ): (T) -> Unit {
        var debounceJob: Job? = null
        return { param: T ->
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(waitMs)
                destinationFunction(param)
            }
        }
    }
*/

    /* @FlowPreview
     @ExperimentalCoroutinesApi
     fun setSearchResult() {
         viewModelScope.launch {
             withContext(Dispatchers.IO) {
                 queryChannel
                     .asFlow()
                     .debounce(500)
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
            _videoMemo.value = if (orderSetting.value == OrderState.MODIFY_RECENT) {
                videos.sortedBy { it.editTime }.reversed()
            } else if (orderSetting.value == OrderState.MODIFY_OLD) {
                videos.sortedBy { it.editTime }
            } else if (orderSetting.value == OrderState.CREATE_OLD) {
                videos.sortedBy { it.createTime }
            } else {
                videos.sortedBy { it.createTime }.reversed()
            }
        }
    }

    private fun getOrderState() {
        _orderSetting.value = sharedPref.getOrderState()
    }

    fun searchVideos(query: String?) {
        query ?: return
        viewModelScope.launch {
            val videos = withContext(Dispatchers.IO) {
                repository.searchVideoByKeyword(query).reversed()
            }
            _videoMemo.value = videos
        }
    }

}