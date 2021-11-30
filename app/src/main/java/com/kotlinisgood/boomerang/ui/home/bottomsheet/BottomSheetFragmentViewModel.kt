package com.kotlinisgood.boomerang.ui.home.bottomsheet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinisgood.boomerang.ui.home.OrderState
import com.kotlinisgood.boomerang.repository.SharedPrefDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BottomSheetFragmentViewModel @Inject constructor(private val sharedPrefDataSource: SharedPrefDataSource) :
    ViewModel() {
    private var _orderSetting = MutableLiveData<OrderState>()
    val orderSetting: LiveData<OrderState> get() = _orderSetting

    private var _createRecent = MutableLiveData<Boolean>()
    val createRecent: LiveData<Boolean> get() = _createRecent

    private var _createOld = MutableLiveData<Boolean>()
    val createOld get() = _createOld

    private var _modifyRecent = MutableLiveData<Boolean>()
    val modifyRecent get() = _modifyRecent

    private var _modifyOld = MutableLiveData<Boolean>()
    val modifyOld get() = _modifyOld


    init {
        getOrderState()
        setCheckBox()
    }

    fun setCheckBox() {
        when (orderSetting.value) {
            OrderState.CREATE_RECENT -> {
                _createRecent.value = true
                _createOld.value = false
                _modifyRecent.value = false
                _modifyOld.value = false
            }
            OrderState.CREATE_OLD -> {
                _createRecent.value = false
                _createOld.value = true
                _modifyRecent.value = false
                _modifyOld.value = false
            }
            OrderState.MODIFY_RECENT -> {
                _createRecent.value = false
                _createOld.value = false
                _modifyRecent.value = true
                _modifyOld.value = false
            }
            else -> {
                _createRecent.value = false
                _createOld.value = false
                _modifyRecent.value = false
                _modifyOld.value = true
            }
        }
    }

    private fun getOrderState() {
        _orderSetting.value = sharedPrefDataSource.getOrderState()
    }

    fun setOrderState(orderState: OrderState) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sharedPrefDataSource.saveOrderState(orderState)
            }
            _orderSetting.value = orderState
        }
    }
}