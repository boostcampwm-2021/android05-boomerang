package com.kotlinisgood.boomerang.ui.trashbin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentTrashBinBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrashBinFragment : Fragment() {
    private lateinit var binding: FragmentTrashBinBinding
    private val viewModel: TrashBinViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_trash_bin,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setBinds()
        setAdapter()

        viewModel.loadTrashVideoMemo()
    }

    private fun setBinds() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setAdapter() {
        binding.rvTrashBinShowVideos.adapter = TrashBinAdapter()
    }
}
