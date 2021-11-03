package com.kotlinisgood.boomerang

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.kotlinisgood.boomerang.database.AppDatabase
import com.kotlinisgood.boomerang.database.dao.VideoMemoDao
import com.kotlinisgood.boomerang.database.entity.VideoMemo
import com.kotlinisgood.boomerang.model.Video
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var dao: VideoMemoDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        lifecycleScope.launch {
//            dao.insertAll(
//                VideoMemo(
//                    id=0,
//                    title="hello world",
//                    videoUri=File(cacheDir, "sample.mp4").toUri().toString(),
//                    memos= emptyList(),
//                    createTime= System.currentTimeMillis(),
//                    editTime = System.currentTimeMillis(),
//                    trashTime = System.currentTimeMillis(),
//                )
//            )
//        }
    }
}