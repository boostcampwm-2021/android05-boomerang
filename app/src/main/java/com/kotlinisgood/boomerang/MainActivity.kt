package com.kotlinisgood.boomerang

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kotlinisgood.boomerang.database.dao.MediaMemoDao
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var dao: MediaMemoDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}