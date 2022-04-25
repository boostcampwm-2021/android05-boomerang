package com.kotlinisgood.boomerang.ui.home.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentBottomSheetBinding
import com.kotlinisgood.boomerang.ui.home.OrderState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentBottomSheetBinding
    private val viewModel: BottomSheetFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewModel()
        setListener()
        setObserver()
    }

    private fun setObserver() {
        viewModel.orderSetting.observe(viewLifecycleOwner) {
            viewModel.setCheckBox()
        }
    }

    private fun setViewModel() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setListener() {
        binding.tvCreateOld.setOnClickListener {
            if (viewModel.orderSetting.value != OrderState.CREATE_OLD) {
                viewModel.setOrderState(OrderState.CREATE_OLD)
                findNavController().navigate(R.id.action_bottomSheetFragment_to_homeFragment)
            }
        }
        binding.tvCreateRecent.setOnClickListener {
            if (viewModel.orderSetting.value != OrderState.CREATE_RECENT) {
                viewModel.setOrderState(OrderState.CREATE_RECENT)
                findNavController().navigate(R.id.action_bottomSheetFragment_to_homeFragment)
            }
        }
        binding.tvEditRecent.setOnClickListener {
            if (viewModel.orderSetting.value != OrderState.MODIFY_RECENT) {
                viewModel.setOrderState(OrderState.MODIFY_RECENT)
                findNavController().navigate(R.id.action_bottomSheetFragment_to_homeFragment)
            }
        }
        binding.tvEditOld.setOnClickListener {
            if (viewModel.orderSetting.value != OrderState.MODIFY_OLD) {
                viewModel.setOrderState(OrderState.MODIFY_OLD)
                findNavController().navigate(R.id.action_bottomSheetFragment_to_homeFragment)
            }
        }
    }
}