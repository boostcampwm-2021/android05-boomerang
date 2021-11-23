package com.kotlinisgood.boomerang.ui.audiorecord

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxbinding4.view.clicks
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentAudioRecordBinding
import com.kotlinisgood.boomerang.util.CustomLoadingDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AudioRecordFragment : Fragment() {
    private val TAG = "AudioRecordSecond"
    private val permissionRejected = "Permission Not Granted By the User"
    private val titleWarning = "타이틀을 입력해주세요"
    private val audioListWarning = "인식된 음성이 존재하지 않습니다"
    private val STTWarning = "구글앱 사용을 활성화해주시거나 구글 앱의 데이터를 삭제한 후 다시 시도해주세요. \n이동하시겠습니까?"
    private val loadingDialog by lazy { CustomLoadingDialog(requireContext()) }

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
                            withContext(Dispatchers.IO) {
                                saveAudio(recognizedText, audioUri)
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
        _dataBinding = FragmentAudioRecordBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _dataBinding?.apply {
            viewModel = viewModel
            lifecycleOwner = viewLifecycleOwner
        }
        setTbSetting()
        setRecognizerListener()
        setObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        _dataBinding = null
    }

    private fun setObserver() {
        viewModel.audioMemo.observe(viewLifecycleOwner) {
            val action = AudioRecordFragmentDirections.actionAudioRecordFragmentToHomeFragment()
            findNavController().navigate(action)
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (loading) {
                loadingDialog.show()
            } else {
                loadingDialog.dismiss()
            }
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

    private fun setTbSetting() {
        dataBinding.tbAudioRecord.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_audio_record_mic -> {
                        checkPermissions()
                        true
                    }
                    R.id.menu_audio_record_save -> {
                        it.clicks()
                            .throttleFirst(1000, TimeUnit.MILLISECONDS)
                            .subscribe {
                                if(dataBinding.etAudioRecordEnterTitle.text.toString() == "") {
                                    Toast.makeText(requireContext(), titleWarning, Toast.LENGTH_SHORT).show()
                                } else if (viewModel.isFileListEmpty()) {
                                    Toast.makeText(requireContext(), audioListWarning, Toast.LENGTH_SHORT).show()
                                } else {
                                    val title = dataBinding.etAudioRecordEnterTitle.text.toString()
                                    loadingDialog.show()
                                    viewModel.saveAudioMemo(title, requireActivity().filesDir)
                                }
                            }
                        true
                    }
                    else -> { false }
                }
            }
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
        try {
            activityCallback.launch(recognizerIntent)
        } catch (e: ActivityNotFoundException) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("구글 앱에 문제가 있습니다.")
                    .setMessage(STTWarning)
                    .setNegativeButton("닫기") { dialog, which ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("이동") { dialog, which ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", "com.google.android.googlequicksearchbox",null)
                        }
                        startActivity(intent)
                        dialog.dismiss()
                    }
                    .show()
        }
    }

    private suspend fun saveAudio(recognizedText: String, audioUri: Uri) {
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