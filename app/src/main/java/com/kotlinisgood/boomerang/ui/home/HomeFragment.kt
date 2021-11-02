package com.kotlinisgood.boomerang.ui.home

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _dataBinding: FragmentHomeBinding? = null
    private val dataBinding get() = _dataBinding!!

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