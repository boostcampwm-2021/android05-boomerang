package com.kotlinisgood.boomerang.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.database.entity.MediaMemo
import com.kotlinisgood.boomerang.repository.AppRepository
import com.kotlinisgood.boomerang.repository.SharedPrefDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val sharedPref: SharedPrefDataSource,
) : ViewModel() {

    private val searchText: PublishSubject<String> = PublishSubject.create()
    private lateinit var searchTextDisposable: Disposable

    private var _mediaMemo = MutableLiveData<List<MediaMemo>>()
    val mediaMemo: LiveData<List<MediaMemo>> = _mediaMemo

    private var _orderSetting = MutableLiveData(OrderState.CREATE_RECENT)
    val orderSetting: LiveData<OrderState> get() = _orderSetting
    private var currentQuery: String = ""

    private var _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        getOrderState()
        setQueryDebounceRxJava(searchText)
    }

    override fun onCleared() {
        super.onCleared()
        searchTextDisposable.dispose()
    }

    private fun setQueryDebounceRxJava(searchText: PublishSubject<String>) {
        searchTextDisposable = searchText
            .debounce(500, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                searchMedia(it.trim())
            }
            .subscribe()
    }

    fun sendQueryRxjava(query: String) {
        if (query.trim() != currentQuery) {
            searchText.onNext(query)
            currentQuery = query.trim()
        }
    }

    fun loadMediaMemo() {
        viewModelScope.launch {
            _isLoading.value = true
            val media = repository.getMediaMemos()
            _mediaMemo.value = when (orderSetting.value) {
                OrderState.MODIFY_RECENT -> {
                    media.sortedBy { it.modifyTime }.reversed()
                }
                OrderState.MODIFY_OLD -> {
                    media.sortedBy { it.modifyTime }
                }
                OrderState.CREATE_OLD -> {
                    media.sortedBy { it.createTime }
                }
                else -> {
                    media.sortedBy { it.createTime }.reversed()
                }
            }
            _isLoading.value = false
        }
    }

    fun loadMediaMemosByType(memoType: Int) {
        viewModelScope.launch {
            val media = withContext(Dispatchers.IO) {
                repository.getMediaMemosByType(memoType)
            }
            _mediaMemo.value = when (orderSetting.value) {
                OrderState.MODIFY_RECENT -> media.sortedBy { it.modifyTime }.reversed()
                OrderState.MODIFY_OLD -> media.sortedBy { it.modifyTime }
                OrderState.CREATE_RECENT -> media.sortedBy { it.createTime }.reversed()
                OrderState.CREATE_OLD -> media.sortedBy { it.createTime }
                else -> media
            }
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

    fun updateMediaMemo(mediaMemo: MediaMemo) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateMediaMemo(mediaMemo)
            }
        }
    }

    fun deleteMediaMemo(mediaMemo: MediaMemo?) {
        mediaMemo?.let { it ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteMemo(it)
            }
        } ?: return
    }

    fun deleteMediaMemo(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemoById(id)
        }
    }
}