package com.kotlinisgood.boomerang.util

import android.media.MediaMetadataRetriever
import android.view.View
import com.google.android.material.snackbar.Snackbar
import java.io.File

object Util {
    fun getDuration(file: File) : String?{
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(file.absolutePath)
        return mmr.run {
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        }
    }

    fun View.showSnackBar(text: String){
        Snackbar.make(this, text, Snackbar.LENGTH_SHORT).show()
    }
}