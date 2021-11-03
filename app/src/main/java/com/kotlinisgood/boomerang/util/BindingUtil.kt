package com.kotlinisgood.boomerang.util

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

@Suppress("UNCHECKED_CAST")
@BindingAdapter("submitList")
fun <T, VH : RecyclerView.ViewHolder> RecyclerView.submitList(list: List<T>?) {
    list?.let {
        (adapter as ListAdapter<T, VH>).submitList(list)
    }
}