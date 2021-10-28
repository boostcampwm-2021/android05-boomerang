package com.kotlinisgood.boomerang.ui.folder.add

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kotlinisgood.boomerang.R

class FolderAddFragment : Fragment() {

    companion object {
        fun newInstance() = FolderAddFragment()
    }

    private lateinit var viewModel: FolderAddViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_folder_add, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(FolderAddViewModel::class.java)
        // TODO: Use the ViewModel
    }

}