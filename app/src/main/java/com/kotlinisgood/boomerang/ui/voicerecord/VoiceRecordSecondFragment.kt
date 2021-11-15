package com.kotlinisgood.boomerang.ui.voicerecord

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.kotlinisgood.boomerang.databinding.FragmentVoiceRecordSecondBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class VoiceRecordSecondFragment : Fragment() {
    private val TAG = "VideoRecordSecond"
    private val permissionRejected = "Permission Not Granted By the User"
    private val VOICE = 1000

    private var _dataBinding: FragmentVoiceRecordSecondBinding? = null
    val dataBinding get() = _dataBinding!!

    private val recognizerIntent by lazy { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH) }

    private val viewModel by viewModels<VoiceRecordViewModel>()

    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.values.contains(false)) {
            Toast.makeText( requireContext(), permissionRejected, Toast.LENGTH_SHORT ).show()
        } else {
            speak()
        }
    }

    private val activityCallback: ActivityResultLauncher<Intent> = selfReference {
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val intent = result.data
                    intent ?: return@ActivityResultCallback
                    val recognizedText: String? = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
                    recognizedText?.let {
                        dataBinding.tvTest.text
                        val audioUri = intent.data as Uri
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                saveAudio(audioUri, it)
                            }
                        }
                    }
                    self.launch(recognizerIntent)
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _dataBinding = FragmentVoiceRecordSecondBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecognizerListener()
        setOnClickListener()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )
        val rejected = mutableListOf<String>()

        permissions.forEach {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    it
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                rejected.add(it)
            }
        }
        if (rejected.isNotEmpty()) {
            permissionsResultCallback.launch(
                rejected.toTypedArray()
            )
        } else {
            speak()
        }
    }

    private fun setRecognizerListener() {
        recognizerIntent.apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireActivity().packageName)
            putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR")
            putExtra("android.speech.extra.GET_AUDIO", true)
        }
    }

    private fun setOnClickListener() {
        dataBinding.btVoiceRecordStartStt.setOnClickListener {
            checkPermissions()
        }
        dataBinding.btVoiceRecordMakeFile.setOnClickListener {
            // FileInputStream
            val voiceList = viewModel.voiceList
            // SequenceInputStream

            // https://stackoverflow.com/questions/35340025/how-to-merge-two-or-more-mp3-audio-file-in-android
        }
    }

    private fun speak() {
        activityCallback.launch(recognizerIntent)
    }

    private fun saveAudio(audioUri: Uri, recognizedText: String) {
        var input: InputStream? = null
        var output: FileOutputStream? = null
        try {
            input = requireActivity().contentResolver.openInputStream(audioUri)

            val fileName = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) + ".amr"
            val file = File(requireActivity().filesDir, fileName)
            output = FileOutputStream(file)

            var read = 0
            val bytes = ByteArray(1024)

            read = input?.read(bytes)!!
            while (read != -1) {
                output.write(bytes, 0, read) ?: break
                read = input.read(bytes)
            }
            MediaMetadataRetriever().apply {
                setDataSource(file.absolutePath)
            }.also {
                val durationStr = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationStr?.let { viewModel.addSubAudio(file.absolutePath, durationStr.toInt(), recognizedText) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                input?.close()
                output?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}