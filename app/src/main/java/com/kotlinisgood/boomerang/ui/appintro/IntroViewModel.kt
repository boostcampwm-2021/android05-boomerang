package com.kotlinisgood.boomerang.ui.appintro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kotlinisgood.boomerang.repository.SharedPrefDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(private val sharedPrefDataSource: SharedPrefDataSource) :
    ViewModel() {
    private val _isFirst: MutableLiveData<Boolean> = MutableLiveData(true)
    val isFirst : LiveData<Boolean> get() = _isFirst

    fun loadIsFirst(){
        println(sharedPrefDataSource.getIsFirst())
        _isFirst.value = sharedPrefDataSource.getIsFirst()
    }

    fun saveIsFirst(){
        sharedPrefDataSource.setIsFirst()
    }
}