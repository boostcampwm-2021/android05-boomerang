package com.kotlinisgood.boomerang

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kotlinisgood.boomerang.database.dao.MediaMemoDao
import com.kotlinisgood.boomerang.ui.appintro.BoomerangIntro
import com.kotlinisgood.boomerang.ui.appintro.IntroViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var dao: MediaMemoDao

    private val viewModel : IntroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)
        setObserver()
        viewModel.loadIsFirst()
    }

    private fun setObserver(){
        viewModel.isFirst.observe(this){
            if(it){
                startIntroActivity()
                finish()
            }
        }
    }
    private fun startIntroActivity(){
        startActivity(Intent(this, BoomerangIntro::class.java))
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val view = currentFocus
        if (view != null && (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_MOVE) && view is EditText && !view.javaClass.name.startsWith("android.webkit.")){
            val scrCoOrds = intArrayOf(0,0)
            view.getLocationOnScreen(scrCoOrds)
            val x = ev.rawX + view.left - scrCoOrds[0]
            val y = ev.rawY + view.top - scrCoOrds[1]
            if(x< view.left || x > view.right || y < view.top || y > view.bottom){
                (this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(this.window.decorView.applicationWindowToken, 0)
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}