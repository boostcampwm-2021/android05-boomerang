package com.kotlinisgood.boomerang.ui.home

import android.Manifest
import android.app.SearchManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.database.AppDatabase
import com.kotlinisgood.boomerang.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val TAG = "HomeFragment"

@AndroidEntryPoint
class HomeFragment : Fragment() {
    @Inject
    lateinit var database: AppDatabase
    private var _dataBinding: FragmentHomeBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val homeAdapter by lazy { MemoListAdapter(requireActivity().contentResolver) }

    private val permissionResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
        when (it) {
            true -> { loadVideos() }
            false -> {
                Toast.makeText(requireContext(),
                    "Permission Not Granted By the User",
                    Toast.LENGTH_SHORT)
                .show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _dataBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        handleIntent(requireActivity().intent)
        dataBinding.rvHomeShowVideos.adapter = homeAdapter

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

    private fun loadVideos() {
        val videoList = mutableListOf<ExternalVideoDTO>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )

        val selection = "${MediaStore.Video.Media.DURATION} <= ?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES).toString()
        )
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        val query = requireActivity().contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                videoList += ExternalVideoDTO(contentUri, name, duration, size)
            }
        }
        Log.i(TAG, videoList.size.toString())
        videoList.forEach { Log.i(TAG, "$it") }
        homeAdapter.submitList(videoList)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_fragment_home, menu)

        val searchView = menu.findItem(R.id.menu_home_search).actionView as SearchView
        searchView.queryHint = getString(R.string.searchable_hint)
        searchView.maxWidth = Int.MAX_VALUE

        val searchManager = requireContext().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val cn = ComponentName("com.kotlinisgood.boomerang", "com.kotlinisgood.boomerang.MainActivity")
        searchView.setSearchableInfo(searchManager.getSearchableInfo(cn))
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEARCH) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            //ToDo Writer: Green / Use the query to search your data
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _dataBinding = null
    }
}