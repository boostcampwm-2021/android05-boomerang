package com.kotlinisgood.boomerang.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File

object Util {
    fun getDuration(file: File) : String?{
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(file.absolutePath)
        return mmr.run {
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        }
    }

    fun Fragment.showToast(text: String){
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }
}