package com.kotlinisgood.boomerang.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentBoomerangLoadingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CustomLoadingDialog(context: Context) : Dialog(context){
    private var _binding : FragmentBoomerangLoadingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.fragment_boomerang_loading, null, false)
        setContentView(binding.root)
        setCancelable(false)
        setAnimation()
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setAnimation() {
        val boomerang = binding.boomerangLoading
        CoroutineScope(Dispatchers.Main).launch {
            delay(300)
            val centerY = (binding.boomerangLoading.top + binding.boomerangLoading.bottom) / 2
            val height = binding.boomerangLoading.height / 2
            val width = binding.boomerangLoading.width / 2

            val right = binding.boomerangLoading.right - width
            val left = binding.boomerangLoading.left - width
            val path = Path().apply {
                arcTo(
                    left.toFloat(),
                    binding.layoutAnimation.top.toFloat(),
                    right.toFloat(),
                    centerY.toFloat() - height,
                    90f,
                    -180f,
                    true
                )
            }
            val animatorSet = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(boomerang, View.X, View.Y, path).apply {
                    duration = binding.boomerangLoading.duration
                })
                play(
                    ObjectAnimator.ofFloat(boomerang, View.SCALE_Y, 0f).apply {
                        duration = binding.boomerangLoading.duration
                    })
                play(ObjectAnimator.ofFloat(boomerang, View.SCALE_X, 0f).apply {
                    duration = binding.boomerangLoading.duration
                })
                start()
            }
            val centerY2 = (binding.boomerangLoading.top + binding.boomerangLoading.bottom) / 2
            val height2 = binding.boomerangLoading.height / 2
            val width2 = binding.boomerangLoading.width / 2

            val right2 = binding.boomerangLoading.right - width2
            val left2 = binding.boomerangLoading.left - width2
            val path2 = Path().apply {
                arcTo(
                    left2.toFloat(),
                    binding.layoutAnimation.top.toFloat(),
                    right2.toFloat(),
                    centerY2.toFloat() - height2,
                    270f,
                    -180f,
                    true
                )
            }
            val animatorSet2 = AnimatorSet().apply{
                play(ObjectAnimator.ofFloat(boomerang, View.X, View.Y, path2).apply {
                    duration = binding.boomerangLoading.duration
                })
                play(
                    ObjectAnimator.ofFloat(boomerang, View.SCALE_Y, 1f).apply {
                        duration = binding.boomerangLoading.duration
                    })
                play(ObjectAnimator.ofFloat(boomerang, View.SCALE_X, 1f).apply {
                    duration = binding.boomerangLoading.duration
                })
            }

            animatorSet.addListener(object: AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    animatorSet2.start()
                }
            })
            animatorSet2.addListener(object: AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    animatorSet.start()
                }
            })
        }
    }
}