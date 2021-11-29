package com.kotlinisgood.boomerang.repository

import android.content.Context
import androidx.core.content.edit
import com.kotlinisgood.boomerang.ui.home.OrderState
import com.kotlinisgood.boomerang.util.IS_FIRST_KEY
import com.kotlinisgood.boomerang.util.ORDER_STATE_KEY
import com.kotlinisgood.boomerang.util.PREF_NAME
import javax.inject.Inject


class SharedPrefDataSource @Inject constructor(context: Context) {

    private val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveOrderState(orderState: OrderState) {
        sharedPref.edit {
            remove(ORDER_STATE_KEY)
            putString(ORDER_STATE_KEY, orderState.order)
        }
    }

    fun getOrderState(): OrderState {
        return when (sharedPref.getString(ORDER_STATE_KEY, "X") ?: "X") {
            "create_recent" -> { OrderState.CREATE_RECENT }
            "create_old" -> {
                OrderState.CREATE_OLD}
            "modify_recent" -> { OrderState.MODIFY_RECENT }
            "modify_old" -> {
                OrderState.MODIFY_OLD}
            else -> { OrderState.CREATE_RECENT }
        }
    }

    fun getIsFirst(): Boolean {
        return when(sharedPref.getBoolean(IS_FIRST_KEY, true)){
            true -> true
            false -> false
        }
    }

    fun setIsFirst(){
        sharedPref.edit{
            remove(IS_FIRST_KEY)
            putBoolean(IS_FIRST_KEY, false)
        }
    }
}