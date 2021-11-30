package com.kotlinisgood.boomerang.ui.appintro

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.kotlinisgood.boomerang.MainActivity
import com.kotlinisgood.boomerang.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BoomerangIntro : AppIntro() {

    private val viewModel: IntroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setColorSkipButton(
            ContextCompat.getColor(
                applicationContext,
                R.color.md_theme_onBackground
            )
        )
        setColorDoneText(ContextCompat.getColor(applicationContext, R.color.md_theme_onBackground))

        isIndicatorEnabled = true
        setIndicatorColor(
            selectedIndicatorColor = ContextCompat.getColor(
                applicationContext,
                R.color.md_theme_onPrimary
            ),
            unselectedIndicatorColor = ContextCompat.getColor(
                applicationContext,
                R.color.md_theme_onBackground
            )
        )

        addSlide(IntroFragment1())
        addSlide(
            AppIntroFragment.newInstance(
                getString(R.string.app_intro_slide_2_title),
                getString(R.string.app_intro_slide_2_description),
                R.drawable.boomerang_intro_2,
                titleColor = ContextCompat.getColor(
                    applicationContext,
                    R.color.md_theme_onBackground
                ),
                descriptionColor = ContextCompat.getColor(
                    applicationContext,
                    R.color.md_theme_onBackground
                )
            )
        )
        addSlide(
            AppIntroFragment.newInstance(
                getString(R.string.app_intro_slide_3_title),
                getString(R.string.app_intro_slide_3_description),
                R.drawable.boomerang_intro_3,
                titleColor = ContextCompat.getColor(
                    applicationContext,
                    R.color.md_theme_onBackground
                ),
                descriptionColor = ContextCompat.getColor(
                    applicationContext,
                    R.color.md_theme_onBackground
                )
            )
        )
        addSlide(
            AppIntroFragment.newInstance(
                getString(R.string.app_intro_slide_4_title),
                getString(R.string.app_intro_slide_4_description),
                R.drawable.boomerang_intro_4,
                titleColor = ContextCompat.getColor(
                    applicationContext,
                    R.color.md_theme_onBackground
                ),
                descriptionColor = ContextCompat.getColor(
                    applicationContext,
                    R.color.md_theme_onBackground
                )
            )
        )
        addSlide(
            AppIntroFragment.newInstance(
                getString(R.string.app_intro_slide_5_title),
                getString(R.string.app_intro_slide_5_description),
                R.drawable.boomerang_intro_5,
                titleColor = ContextCompat.getColor(
                    applicationContext,
                    R.color.md_theme_onBackground
                ),
                descriptionColor = ContextCompat.getColor(
                    applicationContext,
                    R.color.md_theme_onBackground
                )
            )
        )
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        viewModel.saveIsFirst()
        startMainActivity()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        viewModel.saveIsFirst()
        startMainActivity()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}