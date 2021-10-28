package com.kotlinisgood.boomerang.ui.folder.update

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kotlinisgood.boomerang.R

class FolderUpdateFragment : Fragment() {

    companion object {
        fun newInstance() = FolderUpdateFragment()
    }

    private lateinit var viewModel: FolderUpdateViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_folder_update, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(FolderUpdateViewModel::class.java)
        // TODO: Use the ViewModel
    }

}