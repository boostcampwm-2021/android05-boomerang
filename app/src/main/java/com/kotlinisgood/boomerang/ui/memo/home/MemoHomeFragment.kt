package com.kotlinisgood.boomerang.ui.memo.home

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kotlinisgood.boomerang.R

class MemoHomeFragment : Fragment() {

    companion object {
        fun newInstance() = MemoHomeFragment()
    }

    private lateinit var viewModel: MemoHomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_memo_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MemoHomeViewModel::class.java)
        // TODO: Use the ViewModel
    }

}