package com.kotlinisgood.boomerang.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.MediaMemo
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
    private var _mediaMemo = MutableLiveData<List<MediaMemo>>()
    val mediaMemo: LiveData<List<MediaMemo>> = _mediaMemo

    private var _orderSetting = MutableLiveData<OrderState>()
    val orderSetting: LiveData<OrderState> get() = _orderSetting
    private var currentQuery: String = ""

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
                searchMedia(it.trim())
            }
            .subscribe()
    }

    fun sendQueryRxjava(query: String) {
        if(query.trim() != currentQuery) {
            searchText.onNext(query)
            currentQuery = query.trim()
        }
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

    fun loadMediaMemo() {
        viewModelScope.launch {
            val media = repository.getMediaMemos()
            _mediaMemo.value = if (orderSetting.value == OrderState.MODIFY_RECENT) {
                media.sortedBy { it.modifyTime }.reversed()
            } else if (orderSetting.value == OrderState.MODIFY_OLD) {
                media.sortedBy { it.modifyTime }
            } else if (orderSetting.value == OrderState.CREATE_OLD) {
                media.sortedBy { it.createTime }
            } else {
                media.sortedBy { it.createTime }.reversed()
            }
        }
    }

    fun loadMediaMemosByType(memoType: Int) {
        viewModelScope.launch {
            val media = withContext(Dispatchers.IO) {
                repository.getMediaMemosByType(memoType)
            }
            _mediaMemo.value = media
        }
    }

    private fun getOrderState() {
        _orderSetting.value = sharedPref.getOrderState()
    }

    fun searchMedia(query: String?) {
        query ?: return
        viewModelScope.launch {
            val media = withContext(Dispatchers.IO) {
                repository.searchMediaByKeyword(query).reversed()
            }
            _mediaMemo.value = media
        }
    }

}