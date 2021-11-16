package com.kotlinisgood.boomerang.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentHomeBinding
import com.kotlinisgood.boomerang.util.VIDEO_MODE_FRAME
import com.kotlinisgood.boomerang.util.VIDEO_MODE_SUB_VIDEO
import com.leinardi.android.speeddial.SpeedDialActionItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _dataBinding: FragmentHomeBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var homeRecyclerView : RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _dataBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setBinding()
        setAdapter()
        setMenusOnToolbar()

        loadMediaMemo()
        setSpeedDial()
        setSearchMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        _dataBinding = null
    }

    private fun setBinding() {
        dataBinding.viewModel = viewModel
        dataBinding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setAdapter() {
        homeRecyclerView = dataBinding.rvHomeShowMedia
        val adapter = HomeAdapter()
        adapter.setOnItemClickListener(object: HomeAdapter.OnItemClickListener{
            override fun onItemClick(view: View, position: Int) {
                val id = adapter.currentList[position].id
                val action = HomeFragmentDirections.actionHomeFragmentToMemoFragment(id)
                findNavController().navigate(action)
            }
        })
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                homeRecyclerView.layoutManager?.scrollToPosition(0)
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                homeRecyclerView.layoutManager?.scrollToPosition(0)
            }
        })
        homeRecyclerView.adapter = adapter
    }

    private fun loadMediaMemo() {
        viewModel.loadMediaMemo()
    }


    private fun setSearchMenu() {
        val searchView =
            dataBinding.tbHome.menu.findItem(R.id.menu_home_search).actionView as SearchView
        searchView.queryHint = getString(R.string.searchable_hint)
        searchView.maxWidth = Int.MAX_VALUE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchMedia(it) } ?: run {
                    viewModel.loadMediaMemo()
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                newText?: return true
//                viewModel.sendQueryToChannel(newText)
//                viewModel.sendQueryCoroutine(newText)
                viewModel.sendQueryRxjava(newText)
                return true
            }
        })
    }

    private fun setMenusOnToolbar() {
        dataBinding.tbHome.inflateMenu(R.menu.menu_fragment_home)
        dataBinding.tbHome.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_home_order -> {
                    findNavController().navigate(R.id.action_homeFragment_to_bottomSheetFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun setSpeedDial() {
        dataBinding.sdHomeShowItems.addAllActionItems(
            listOf(
                SpeedDialActionItem.Builder(R.id.menu_home_sd_video, R.drawable.image_our_memo)
                    .setFabBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                    .setLabel(R.string.menu_home_sd_video)
                    .create(),
                SpeedDialActionItem.Builder(
                    R.id.menu_home_sd_video_light,
                    R.drawable.image_my_memo
                )
                    .setFabBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                    .setLabel(R.string.menu_home_sd_video_light)
                    .create(),
            )
        )
        dataBinding.sdHomeShowItems.setOnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.menu_home_sd_video -> {
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeFragmentToVideoSelectionFragment(
                            VIDEO_MODE_FRAME
                        )
                    )
                }
                R.id.menu_home_sd_video_light -> {
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeFragmentToVideoSelectionFragment(
                            VIDEO_MODE_SUB_VIDEO
                        )
                    )
                }
            }
            false
        }
    }

    companion object {
        const val TAG = "HomeFragment"
    }
}