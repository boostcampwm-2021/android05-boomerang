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
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentHomeBinding
import com.kotlinisgood.boomerang.model.OrderState
import com.kotlinisgood.boomerang.util.VIDEO_MODE_FRAME
import com.kotlinisgood.boomerang.util.VIDEO_MODE_SUB_VIDEO
import com.leinardi.android.speeddial.SpeedDialActionItem
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _dataBinding: FragmentHomeBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val viewModel: HomeViewModel by viewModels()

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

        loadVideoMemo()
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
        dataBinding.rvHomeShowVideos.adapter = HomeAdapter()
    }

    private fun loadVideoMemo() {
        viewModel.loadVideoMemo()
    }
    // 코틀린 코루틴 debounce 공부
/*    private fun <T> debounce(
        waitMs: Long,
        scope: CoroutineScope,
        destinationFunction: (T) -> Unit
    ): (T) -> Unit {
        var debounceJob: Job? = null
        return { param: T ->
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(waitMs)
                destinationFunction(param)
            }
        }
    }*/

    private fun setSearchMenu() {
        val searchView =
            dataBinding.tbHome.menu.findItem(R.id.menu_home_search).actionView as SearchView
        searchView.queryHint = getString(R.string.searchable_hint)
        searchView.maxWidth = Int.MAX_VALUE

        val searchText: PublishSubject<String> = PublishSubject.create()
//        val kotlinDebounceText = debounce(1000L, lifecycleScope, viewModel::searchVideos)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchVideos(it) } ?: run {
                    viewModel.loadVideoMemo()
                }
                return true
            }
            // newText를 값을 flow로 받는다 계속 받는다  searchVideos(query)
            // newText 의 값을 BroadcastChannel queue -> 값을 보내요
            // channel이 flow가 되는 거에요
            override fun onQueryTextChange(newText: String?): Boolean {
                newText?: return true
//                viewModel.sendQueryToChannel(newText)
                searchText.onNext(newText)
//                kotlinDebounceText(newText)
                return true
            }
        })
        searchText
            .debounce(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext{
                viewModel.searchVideos(it)
            }
            .subscribe()
    }

    private fun setMenusOnToolbar() {
        dataBinding.tbHome.inflateMenu(R.menu.menu_fragment_home)
        dataBinding.tbHome.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_home_order_create -> {
                    if (viewModel.orderSetting.value != OrderState.CREATE) {
                        viewModel.setOrderState(OrderState.CREATE)
                    }
                    true
                }
                R.id.menu_home_order_modify -> {
                    if (viewModel.orderSetting.value != OrderState.MODIFY) {
                        viewModel.setOrderState(OrderState.MODIFY)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setSpeedDial() {
        dataBinding.sdHomeShowItems.addAllActionItems(
            listOf(
                SpeedDialActionItem.Builder(R.id.menu_home_sd_video, R.drawable.ic_video)
                    .setFabBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                    .setLabel(R.string.menu_home_sd_video)
                    .create(),
                SpeedDialActionItem.Builder(
                    R.id.menu_home_sd_video_light,
                    R.drawable.ic_video_light
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
        const val PACKAGE_NAME = "com.kotlinisgood.boomerang"
        const val MAIN_ACTIVITY = "com.kotlinisgood.boomerang.MainActivity"
    }
}