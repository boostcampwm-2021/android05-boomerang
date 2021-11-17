package com.kotlinisgood.boomerang.ui.audiorecord

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
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
import androidx.navigation.fragment.findNavController
import com.kotlinisgood.boomerang.databinding.FragmentAudioRecordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*

@AndroidEntryPoint
class AudioRecordFragment : Fragment() {
    private val TAG = "AudioRecordSecond"
    private val permissionRejected = "Permission Not Granted By the User"
    private val titleWarning = "타이틀을 입력해주세요"
    private val VOICE = 1000

    private var _dataBinding: FragmentAudioRecordBinding? = null
    val dataBinding get() = _dataBinding!!

    private val recognizerIntent by lazy { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH) }

    private val viewModel: AudioRecordViewModel by viewModels()

    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.values.contains(false)) {
            Toast.makeText(requireContext(), permissionRejected, Toast.LENGTH_SHORT).show()
        } else {
            startSTT()
        }
    }

    private val activityCallback: ActivityResultLauncher<Intent> = selfReference {
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val intent = result.data
                    intent ?: return@ActivityResultCallback
                    val recognizedText: String? =
                        intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
                    recognizedText?.let {
                        val audioUri = intent.data as Uri
                        lifecycleScope.launch {
                            saveAudio(audioUri, it)
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
        _dataBinding = FragmentAudioRecordBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _dataBinding?.apply {
            viewModel = viewModel
            lifecycleOwner = viewLifecycleOwner
        }
        setRecognizerListener()
        setOnClickListener()
        setObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        _dataBinding = null
    }

    private fun setObserver() {
        viewModel.audioMemo.observe(viewLifecycleOwner) {
            val action = AudioRecordFragmentDirections.actionAudioRecordFragmentToAudioMemoFragment(it.id)
            findNavController().navigate(action)
        }
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
            startSTT()
        }
    }

    private fun startSTT() {
        activityCallback.launch(recognizerIntent)
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
            if (dataBinding.etAudioRecordEnterTitle.text.toString() == "") {
                Toast.makeText(it.context, titleWarning, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val title = dataBinding.etAudioRecordEnterTitle.text.toString()
            viewModel.saveAudioMemo(title, requireActivity().filesDir)
            // https://stackoverflow.com/questions/35340025/how-to-merge-two-or-more-mp3-audio-file-in-android
        }
    }

    private fun saveAudio(audioUri: Uri, recognizedText: String) {
        Log.i(TAG, "save audio is called")
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                saveFirstAudio(recognizedText, audioUri)
            }
        }
    }

    private suspend fun saveFirstAudio(recognizedText: String, audioUri: Uri) {
        Log.i(TAG, "save audio is called")
        var input: InputStream? = null
        var output: FileOutputStream? = null
        try {
            input = requireActivity().contentResolver.openInputStream(audioUri)

            val createTime = System.currentTimeMillis()
            val fileName = "$createTime.mp3"
            val file = File(requireActivity().filesDir, fileName)
            output = FileOutputStream(file)

            var read = 0
            val bytes = ByteArray(1024)
            read = input?.read(bytes)!!
            while (read != -1) {
                output.write(bytes, 0, read)
                read = input.read(bytes)
            }

            getDuration(file)?.let {
                Log.i(TAG, "save audio's duration $it")
                withContext(Dispatchers.Main) {
                    viewModel.addTimeAndText(recognizedText, it.toInt())
                    dataBinding.tvAudioRecordShowRecognizedText.text =  viewModel.getAccText()
                    viewModel.addFileToList(file)
                }
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

    private fun getDuration(file: File): String? {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(file.absolutePath)
        return mmr.run {
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        }
    }

}