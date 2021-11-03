package com.kotlinisgood.boomerang.ui.videoselection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentVideoSelectionBinding
import com.kotlinisgood.boomerang.ui.home.VideoGallery
import com.kotlinisgood.boomerang.util.UriUtil

class VideoSelectionFragment : Fragment() {
    private var _dataBinding: FragmentVideoSelectionBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val videoSelectionAdapter by lazy {
        VideoSelectionAdapter(requireActivity().contentResolver)
    }
    private val permissionResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        when (it) {
            true -> {
                loadVideos()
            }
            false -> {
                Toast.makeText(
                    requireContext(),
                    "Permission Not Granted By the User",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    fun checkPermission() {
        val readPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (readPermission != PackageManager.PERMISSION_GRANTED) {
            permissionResultCallback.launch(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            loadVideos()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _dataBinding = FragmentVideoSelectionBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        dataBinding.rvVideoSelectionShowVideos.adapter = videoSelectionAdapter
        setTbNavigationIconClickListener()
        checkPermission()

        setOnMenuItemClickListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        _dataBinding = null
    }

    fun setTbNavigationIconClickListener() {
        dataBinding.tbVideoSelection.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    fun loadVideos() {
        videoSelectionAdapter.submitList(VideoGallery(requireActivity().contentResolver).loadVideos())
    }

    fun setOnMenuItemClickListener() {
        dataBinding.tbVideoSelection.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_video_selection_completion -> {
                    if (videoSelectionAdapter.selectedIndex == -1) {
                        Toast.makeText(requireContext(), "동영상이 선택되지 않았습니다", Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        val uri = videoSelectionAdapter.currentList[videoSelectionAdapter.selectedIndex].uri
                        val path = UriUtil.getPathFromUri(requireActivity().contentResolver, uri)
                        val action =
                            VideoSelectionFragmentDirections
                                .actionVideoSelectionFragmentToVideoDoodleLightFragment(path)
                        findNavController().navigate(action)
                        true
                    }
                }
                else -> false
            }
        }
    }

}