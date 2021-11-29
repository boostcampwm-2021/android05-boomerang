package com.kotlinisgood.boomerang.ui.audiorecord

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kotlinisgood.boomerang.R
import com.kotlinisgood.boomerang.databinding.FragmentAudioRecordBinding
import com.kotlinisgood.boomerang.util.CustomLoadingDialog
import com.kotlinisgood.boomerang.util.Util
import com.kotlinisgood.boomerang.util.throttle
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AudioRecordFragment : Fragment() {

    private val loadingDialog by lazy { CustomLoadingDialog(requireContext()) }

    private val compositeDisposable by lazy { CompositeDisposable() }

    private var _dataBinding: FragmentAudioRecordBinding? = null
    val dataBinding get() = _dataBinding!!

    private val recognizerIntent by lazy { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH) }

    private val audioRecordViewModel: AudioRecordViewModel by viewModels()

    private val permissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
    )
    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.values.contains(false)) {
            Toast.makeText(requireContext(), getString(R.string.permission_rejected), Toast.LENGTH_SHORT).show()
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
    ): View {
        _dataBinding = FragmentAudioRecordBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBinding()
        setTbSetting()
        setRecognizerIntentAttr()
        setObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        _dataBinding = null
        if (!audioRecordViewModel.isFileListEmpty()) { audioRecordViewModel.deleteAudios() }
        compositeDisposable.dispose()
    }

    private fun setBinding() {
        _dataBinding?.apply {
            viewModel = audioRecordViewModel
            lifecycleOwner = viewLifecycleOwner
        }
    }

    private fun setObserver() {
        audioRecordViewModel.audioMemo.observe(viewLifecycleOwner) {
            val action = AudioRecordFragmentDirections.actionAudioRecordFragmentToHomeFragment()
            findNavController().navigate(action)
        }
        audioRecordViewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (loading) {
                loadingDialog.show()
            } else {
                loadingDialog.dismiss()
            }
        }
    }

    private fun setRecognizerIntentAttr() {
        recognizerIntent.apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireActivity().packageName)
            putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR")
            putExtra("android.speech.extra.GET_AUDIO", true)
        }
    }

    private fun setTbSetting() {
        dataBinding.tbAudioRecord.apply {
            compositeDisposable.add(throttle(1000,TimeUnit.MILLISECONDS) {
                findNavController().popBackStack()
            })
            menu.forEach {
                when (it.itemId) {
                    R.id.menu_audio_record_mic -> {
                        compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) { checkPermissions() })
                    }
                    R.id.menu_audio_record_save -> {
                        compositeDisposable.add(it.throttle(1000, TimeUnit.MILLISECONDS) { checkAudioAndSave() })
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
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
            e.printStackTrace()
            showGoogleErrorDialog()
        }
    }

    private fun checkAudioAndSave() {
        if(dataBinding.etAudioRecordEnterTitle.text.toString() == "") {
            Toast.makeText(requireContext(), getString(R.string.title_warning), Toast.LENGTH_SHORT).show()
        } else if (audioRecordViewModel.isFileListEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.audio_list_warning), Toast.LENGTH_SHORT).show()
        } else {
            val title = dataBinding.etAudioRecordEnterTitle.text.toString()
            loadingDialog.show()
            audioRecordViewModel.saveAudioMemo(title, requireActivity().filesDir)
        }
    }

    private fun showGoogleErrorDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.google_warning_title))
            .setMessage(getString(R.string.stt_warning))
            .setNegativeButton(getString(R.string.dialog_negative_close)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.dialog_positive_move)) { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", "com.google.android.googlequicksearchbox",null)
                }
                startActivity(intent)
                dialog.dismiss()
            }
            .show()
    }

    private suspend fun saveAudio(recognizedText: String, audioUri: Uri) {
        var input: InputStream? = null
        var output: FileOutputStream? = null
        try {
            input = requireActivity().contentResolver.openInputStream(audioUri)

            val createTime = System.currentTimeMillis()
            val fileName = "$createTime.mp3"
            val file = File(requireActivity().filesDir, fileName)
            output = FileOutputStream(file)

            val bytes = ByteArray(1024)
            var read = input?.read(bytes)!!
            while (read != -1) {
                output.write(bytes, 0, read)
                read = input.read(bytes)
            }

            Util.getDuration(file)?.let {
                withContext(Dispatchers.Main) {
                    audioRecordViewModel.addTimeAndText(recognizedText, it.toInt())
                    dataBinding.tvAudioRecordShowRecognizedText.text =  audioRecordViewModel.getAccText()
                    audioRecordViewModel.addFileToList(file)
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

}