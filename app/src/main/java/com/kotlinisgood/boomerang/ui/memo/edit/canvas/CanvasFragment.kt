package com.kotlinisgood.boomerang.ui.memo.edit.canvas

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kotlinisgood.boomerang.R

class CanvasFragment : Fragment() {

    companion object {
        fun newInstance() = CanvasFragment()
    }

    private lateinit var viewModel: CanvasViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_canvas, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CanvasViewModel::class.java)
        // TODO: Use the ViewModel
    }

}