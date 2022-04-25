package com.kotlinisgood.boomerang.util

import android.content.ContentResolver
import android.net.Uri

object UriUtil {

    fun getPathFromUri(contentResolver: ContentResolver, uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null) ?: return ""
        cursor.moveToFirst()
        val path = cursor.getString(cursor.getColumnIndexOrThrow("_data"))
        cursor.close()
        return path
    }

}