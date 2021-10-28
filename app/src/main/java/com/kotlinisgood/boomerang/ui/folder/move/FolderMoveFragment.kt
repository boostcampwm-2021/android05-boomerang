package com.kotlinisgood.boomerang.ui.folder.move

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kotlinisgood.boomerang.R

class FolderMoveFragment : Fragment() {

    companion object {
        fun newInstance() = FolderMoveFragment()
    }

    private lateinit var viewModel: FolderMoveViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_folder_move, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(FolderMoveViewModel::class.java)
        // TODO: Use the ViewModel
    }

}