package com.kotlinisgood.boomerang.ui.setting.oss

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kotlinisgood.boomerang.R

class OssFragment : Fragment() {

    companion object {
        fun newInstance() = OssFragment()
    }

    private lateinit var viewModel: OssViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_oss, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(OssViewModel::class.java)
        // TODO: Use the ViewModel
    }

}