package com.kotlinisgood.boomerang

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.room.ColumnInfo
import com.kotlinisgood.boomerang.database.AppDatabase
import com.kotlinisgood.boomerang.database.entity.VideoMemo
import com.kotlinisgood.boomerang.model.Video
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}