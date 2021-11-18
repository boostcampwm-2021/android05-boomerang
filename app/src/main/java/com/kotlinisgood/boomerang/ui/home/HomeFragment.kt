package com.kotlinisgood.boomerang.ui.home

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentHomeBinding
import com.kotlinisgood.boomerang.util.AUDIO_MODE
import com.kotlinisgood.boomerang.util.VIDEO_MODE_FRAME
import com.kotlinisgood.boomerang.util.VIDEO_MODE_SUB_VIDEO
import com.leinardi.android.speeddial.SpeedDialActionItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _dataBinding: FragmentHomeBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val viewModel: HomeViewModel by viewModels()
    private var sglm: StaggeredGridLayoutManager? = null
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
        getStaggeredGridLayoutManager()
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
                val item = adapter.currentList[position]
                val action = if (item.memoType == AUDIO_MODE) {
                    HomeFragmentDirections.actionHomeFragmentToAudioMemoFragment(item.id)
                } else {
                    HomeFragmentDirections.actionHomeFragmentToMemoFragment(item.id)
                }
                findNavController().navigate(action)
            }
        })
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                getStaggeredGridLayoutManager()
                homeRecyclerView.layoutManager?.scrollToPosition(0)
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                getStaggeredGridLayoutManager()
                homeRecyclerView.layoutManager?.scrollToPosition(0)
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                getStaggeredGridLayoutManager()
                homeRecyclerView.layoutManager?.scrollToPosition(0)
            }
        })
        homeRecyclerView.adapter = adapter
        homeRecyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                sglm?.invalidateSpanAssignments()
            }
        })
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
        dataBinding.tbHome.setNavigationIcon(R.drawable.ic_menu)
        dataBinding.tbHome.setNavigationOnClickListener {
            dataBinding.drawerLayout.openDrawer(GravityCompat.START)
        }
        dataBinding.navigationView.setNavigationItemSelectedListener {
            println(it)
//            오픈소스 라이선스 제외하고 true로 해야함. 현재 선택한 메모 상태를 보여줄 수 있음
            it.isChecked = true
            dataBinding.drawerLayout.close()
            true
        }
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
                SpeedDialActionItem.Builder(
                    R.id.menu_home_sd_audio,
                    R.drawable.image_audio
                )
                    .setFabBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                    .setLabel(R.string.menu_home_sd_audio)
                    .create()
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
                R.id.menu_home_sd_audio -> {
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeFragmentToAudioRecordFragment()
                    )
                }
            }
            false
        }
    }

    fun getStaggeredGridLayoutManager() {
        sglm = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }.also {
            dataBinding.rvHomeShowMedia.layoutManager = it
        }
    }

    companion object {
        const val TAG = "HomeFragment"
    }
}