package com.kotlinisgood.boomerang.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kotlinisgood.boomerang.databinding.FragmentBoomerangThrowBinding
import com.kotlinisgood.boomerang.util.VIDEO_MODE_FRAME
import com.kotlinisgood.boomerang.util.VIDEO_MODE_SUB_VIDEO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BoomerangThrowAnimation : DialogFragment() {
    private lateinit var binding: FragmentBoomerangThrowBinding
    private val args: BoomerangThrowAnimationArgs by navArgs()
    private lateinit var animatorSet: AnimatorSet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBoomerangThrowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAnimation()
    }

    override fun onPause() {
        super.onPause()
        animatorSet.pause()
    }

    private fun setAnimation() {
        val boomerang = binding.animationView
        CoroutineScope(Dispatchers.Main).launch {
            delay(300)
            val centerY = (binding.animationView.top + binding.animationView.bottom) / 2
            val height = binding.animationView.height / 2
            val width = binding.animationView.width / 2

            val right = binding.animationView.right - width
            val left = binding.animationView.left - width
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
            animatorSet = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(boomerang, View.X, View.Y, path).apply {
                    duration = binding.animationView.duration
                })
                play(
                    ObjectAnimator.ofFloat(boomerang, View.SCALE_Y, 0f).apply {
                        duration = binding.animationView.duration
                    })
                play(ObjectAnimator.ofFloat(boomerang, View.SCALE_X, 0f).apply {
                    duration = binding.animationView.duration
                })
                start()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        if (args.memoType == VIDEO_MODE_FRAME) {
                            val action =
                                BoomerangThrowAnimationDirections.actionBoomerangThrowAnimationToVideoDoodleFragment(
                                    args.videoPath
                                )
                            findNavController().navigate(action)
                        } else if (args.memoType == VIDEO_MODE_SUB_VIDEO) {
                            val action =
                                BoomerangThrowAnimationDirections.actionBoomerangThrowAnimationToVideoDoodleLightFragment(
                                    args.videoPath
                                )
                            findNavController().navigate(action)
                        }
                    }
                })
            }
        }
    }
}