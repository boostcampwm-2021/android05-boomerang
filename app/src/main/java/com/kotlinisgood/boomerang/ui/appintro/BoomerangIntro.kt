package com.kotlinisgood.boomerang.ui.appintro

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.kotlinisgood.boomerang.MainActivity
import com.kotlinisgood.boomerang.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BoomerangIntro : AppIntro() {

    private val viewModel : IntroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isIndicatorEnabled = true
        setColorSkipButton(R.color.black)
        setColorDoneText(R.color.black)
        setIndicatorColor(
            selectedIndicatorColor = R.color.black,
            unselectedIndicatorColor = R.color.recognized_text_no_focus
        )
        addSlide(IntroFragment1())
        addSlide(AppIntroFragment.newInstance(
            "나만의 메모",
            "원하는 곳에 메모를 할 수 있어요",
            R.drawable.boomerang_intro_2,
            titleColor = Color.BLACK,
            descriptionColor = Color.BLACK
        ))
        addSlide(AppIntroFragment.newInstance(
            "모두의 메모",
            "다른 사람과 공유를 할 수 있어요",
            R.drawable.boomerang_intro_3,
            titleColor = Color.BLACK,
            descriptionColor = Color.BLACK
        ))
        addSlide(AppIntroFragment.newInstance(
            "음성 메모",
            "녹음을 멜론처럼 텍스트와 들을 수 있어요",
            R.drawable.boomerang_intro_4,
            titleColor = Color.BLACK,
            descriptionColor = Color.BLACK
        ))
        addSlide(AppIntroFragment.newInstance(
            "부메랑 시작하기!",
        "신개념 메모하러 가자!",
            R.drawable.boomerang_intro_5,
            titleColor = Color.BLACK,
            descriptionColor = Color.BLACK
        ))


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

    private fun startMainActivity(){
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}