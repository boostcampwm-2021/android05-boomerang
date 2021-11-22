package com.kotlinisgood.boomerang.util

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.kotlinisgood.boomerang.R

class CustomLoadingDialog(context: Context) : Dialog(context){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_boomerang_loading)
        setCancelable(false)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

}