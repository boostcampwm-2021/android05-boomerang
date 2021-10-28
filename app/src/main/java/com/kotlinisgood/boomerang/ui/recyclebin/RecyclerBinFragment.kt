package com.kotlinisgood.boomerang.ui.recyclebin

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kotlinisgood.boomerang.R

class RecyclerBinFragment : Fragment() {

    companion object {
        fun newInstance() = RecyclerBinFragment()
    }

    private lateinit var viewModel: RecyclerBinViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recycler_bin, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(RecyclerBinViewModel::class.java)
        // TODO: Use the ViewModel
    }

}