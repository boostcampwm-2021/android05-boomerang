package com.kotlinisgood.boomerang.repository

import android.content.Context
import androidx.core.content.edit
import com.kotlinisgood.boomerang.model.OrderState
import com.kotlinisgood.boomerang.util.ORDER_STATE
import com.kotlinisgood.boomerang.util.PREF_NAME
import javax.inject.Inject


class SharedPrefDataSource @Inject constructor(context: Context) {

    private val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveOrderState(orderState: OrderState) {
        sharedPref.edit {
            remove(ORDER_STATE)
            putString(ORDER_STATE, orderState.order)
        }
    }

    fun getOrderState(): OrderState {
        return when (sharedPref.getString(ORDER_STATE, "X") ?: "X") {
            "create" -> { OrderState.CREATE }
            "modify" -> { OrderState.MODIFY }
            else -> { OrderState.NONE }
        }
    }
}