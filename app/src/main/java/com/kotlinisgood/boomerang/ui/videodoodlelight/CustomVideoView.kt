package com.kotlinisgood.boomerang.ui.videodoodlelight

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

class CustomVideoView(context: Context,attr: AttributeSet) :VideoView(context,attr) {


    private lateinit var listener: PlayPauseListener

    fun setPlayPauseListener(listener : PlayPauseListener){
        this.listener = listener
    }

    override fun pause() {
        super.pause()
        if(this::listener.isInitialized){
            listener.onPause()
        }
    }
    override fun start() {
        super.start()
        if (this::listener.isInitialized){
            listener.onPlay()
        }
    }

    interface PlayPauseListener{
        fun onPlay()
        fun onPause()
    }
}