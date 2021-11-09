package com.kotlinisgood.boomerang.ui.videoedit

import android.content.Context
import android.util.Xml
import android.view.ViewGroup
import androidx.core.net.toUri
import com.alphamovie.lib.AlphaMovieView
import com.kotlinisgood.boomerang.R

class AlphaViewFactory(val context: Context) {
    fun create(): AlphaMovieView {
        val xpp = context.resources.getXml(R.xml.alpha_movie_view)
        xpp.next()
        xpp.nextTag()
        val attr = Xml.asAttributeSet(xpp)
        val alphaMovieView = AlphaMovieView(context, attr)
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        alphaMovieView.layoutParams = layoutParams
        alphaMovieView.setLooping(false)
        return alphaMovieView
    }

}