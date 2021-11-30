package com.kotlinisgood.boomerang.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentHomeBinding
import com.kotlinisgood.boomerang.util.*
import com.leinardi.android.speeddial.SpeedDialActionItem
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _dataBinding: FragmentHomeBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val viewModel: HomeViewModel by viewModels()
    private var sglm: StaggeredGridLayoutManager? = null
    private lateinit var homeRecyclerView: RecyclerView
    private val loadingDialog by lazy { CustomLoadingDialog(requireContext()) }
    private val compositeDisposable by lazy { CompositeDisposable() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadMediaMemo()
    }

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
        setMenuInflate()
        setMenusOnToolbar()
        setSpeedDial()
        setSearchMenu()
        setLoadingObserver()
    }

    private fun setLoadingObserver() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading) {
                loadingDialog.show()
            } else {
                loadingDialog.dismiss()
            }
        }
        viewModel.mediaMemo.observe(viewLifecycleOwner) { memoList ->
            if (memoList.isNullOrEmpty()) {
                dataBinding.layoutEmptyAnimation.visibility = View.VISIBLE
            } else {
                dataBinding.layoutEmptyAnimation.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _dataBinding = null
        compositeDisposable.dispose()
    }

    private fun setBinding() {
        dataBinding.viewModel = viewModel
        dataBinding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setAdapter() {
        homeRecyclerView = dataBinding.rvHomeShowMedia
        val adapter = HomeAdapter(viewModel)
        adapter.setOnItemClickListener(object : HomeAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val item = adapter.currentList[position]
                val action = if (item.memoType == AUDIO_MODE) {
                    HomeFragmentDirections.actionHomeFragmentToAudioMemoFragment(item.id)
                } else {
                    HomeFragmentDirections.actionHomeFragmentToMemoFragment(item.id, item.memoType)
                }
                findNavController().navigate(action)
            }
        })
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
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
        homeRecyclerView.itemAnimator = null
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
                val imm =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText ?: return true
                viewModel.sendQueryRxjava(newText)
                return true
            }
        })
    }

    private fun setMenuInflate() {
        dataBinding.tbHome.inflateMenu(R.menu.menu_fragment_home)
    }

    private fun setMenusOnToolbar() {
        dataBinding.tbHome.apply {
            setNavigationIcon(R.drawable.ic_menu)
            compositeDisposable.add(throttle(1000, TimeUnit.MILLISECONDS) {
                dataBinding.drawerLayout.openDrawer(GravityCompat.START)
            })
            menu.forEach {
                when (it.itemId) {
                    R.id.menu_home_order -> {
                        compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) {
                            findNavController().navigate(R.id.action_homeFragment_to_bottomSheetFragment)
                        })
                    }
                }
            }
        }
        dataBinding.navigationView.setCheckedItem(R.id.navigation_drawer_memo_type_all)
        dataBinding.navigationView.setNavigationItemSelectedListener {
//            오픈소스 라이선스 제외하고 true로 해야함. 현재 선택한 메모 상태를 보여줄 수 있음
            when (it.itemId) {
                R.id.navigation_drawer_memo_type_all -> {
                    viewModel.loadMediaMemo()
                }
                R.id.navigation_drawer_memo_type_voice -> {
                    viewModel.loadMediaMemosByType(AUDIO_MODE)
                }
                R.id.navigation_drawer_memo_type_my_video -> {
                    viewModel.loadMediaMemosByType(VIDEO_MODE_SUB_VIDEO)
                }
                R.id.navigation_drawer_memo_type_together_video -> {
                    viewModel.loadMediaMemosByType(VIDEO_MODE_FRAME)
                }
                R.id.navigation_drawer_option_open_source -> {
                    startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
                }
            }
            dataBinding.drawerLayout.close()
            true
        }
    }

    private fun setSpeedDial() {
        dataBinding.sdHomeShowItems.addAllActionItems(
            listOf(
                SpeedDialActionItem.Builder(R.id.menu_home_sd_video, R.drawable.ic_people)
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.md_theme_primary
                        )
                    )
                    .setFabImageTintColor(ContextCompat.getColor(requireContext(), R.color.black))
                    .setLabel(R.string.menu_home_sd_video)
                    .create(),
                SpeedDialActionItem.Builder(
                    R.id.menu_home_sd_video_light,
                    R.drawable.ic_person
                )
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.md_theme_primary
                        )
                    )
                    .setFabImageTintColor(ContextCompat.getColor(requireContext(), R.color.black))
                    .setLabel(R.string.menu_home_sd_video_light)
                    .create(),
                SpeedDialActionItem.Builder(
                    R.id.menu_home_sd_audio,
                    R.drawable.ic_voice
                )
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.md_theme_primary
                        )
                    )
                    .setFabImageTintColor(ContextCompat.getColor(requireContext(), R.color.black))
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
        }.also {
            dataBinding.rvHomeShowMedia.layoutManager = it
        }
    }

    companion object {
        const val TAG = "HomeFragment"
    }
}