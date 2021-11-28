package com.kotlinisgood.boomerang.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri

object UriUtil {

    @SuppressLint("Range")
    fun getPathFromUri(contentResolver: ContentResolver, uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null) ?: return ""
        cursor.moveToFirst()
        val path = cursor.getString(cursor.getColumnIndex("_data"))
        cursor.close()
        return path
    }

}