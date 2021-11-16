package com.kotlinisgood.boomerang.ui.audiorecord

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kotlinisgood.boomerang.databinding.FragmentVoiceRecordBinding

class VoiceRecordFragment : Fragment(), RecognitionListener {
    private val TAG: String = "VoiceRecord"
    private var speech: SpeechRecognizer? = null
    private val recognizer get() = speech!!
    private var recordingState = false
    private var _dataBinding: FragmentVoiceRecordBinding? = null
    private val dataBinding get() = _dataBinding!!
    private val recognizerIntent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    }
    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.values.contains(false)) {
            Toast.makeText(
                requireContext(),
                "권한이 없습니다",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _dataBinding = FragmentVoiceRecordBinding.inflate(inflater, container, false)
        return dataBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetSpeechRecognizer()
        setOnClickListener()
    }

    override fun onResume() {
        super.onResume()
        resetSpeechRecognizer()
    }

    override fun onPause() {
        super.onPause()
        recognizer.stopListening()
    }

    override fun onStop() {
        super.onStop()
        if (speech != null) {
            speech?.destroy()
        }
    }

    private fun setOnClickListener() {
        dataBinding.btTest.setOnClickListener {
            checkPermissions()
            recordingState = true
            setRecognizerIntent()
            recognizer.startListening(recognizerIntent)
        }

        dataBinding.btStop.setOnClickListener {
            recordingState = false
            recognizer.stopListening()
            Log.i(TAG, "Intent Data ${recognizerIntent.data}")
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
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
        rejected.forEach {
            println(it)
        }
        if (rejected.isNotEmpty()) {
            permissionsResultCallback.launch(
                rejected.toTypedArray()
            )
        }
    }

    private fun resetSpeechRecognizer() {
        speech?.destroy()
        Log.i(
            TAG,
            "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(requireContext())
        )
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speech = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
                setRecognitionListener(this@VoiceRecordFragment)
            }
        }
    }

    private fun setRecognizerIntent() {
        recognizerIntent.apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "kr")
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireActivity().packageName)
            putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR")
            putExtra("android.speech.extra.GET_AUDIO", true)
        }
    }

    // Recognition Listener
    override fun onReadyForSpeech(params: Bundle?) {
        Log.i(TAG, "onReadyForSpeech")
    }

    // Recognition Listener
    override fun onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech")
    }

    // Recognition Listener
    override fun onRmsChanged(rmsdB: Float) {

    }

    // Recognition Listener
    override fun onBufferReceived(buffer: ByteArray?) {
        Log.i(TAG, "onBufferReceived: $buffer")
    }

    // Recognition Listener
    override fun onEndOfSpeech() {
        Log.i(TAG, "onEndOfSpeech")
        recognizer.stopListening()
    }

    // Recognition Listener
    override fun onError(error: Int) {
        Log.i(TAG, "Error : ${getErrorText(error)}")
        resetSpeechRecognizer()
        if (recordingState) recognizer.startListening(recognizerIntent)
        else recognizer.stopListening()
    }

    // Recognition Listener
    override fun onResults(results: Bundle?) {
        Log.i(TAG, "onResults")
        val matchResults = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val result = matchResults?.get(0) ?: "Return Null"
        dataBinding.tvTest.text = result

        if (recordingState) recognizer.startListening(recognizerIntent)
        else recognizer.stopListening()
    }

    // Recognition Listener
    override fun onPartialResults(partialResults: Bundle?) {

    }

    // Recognition Listener
    override fun onEvent(eventType: Int, params: Bundle?) {

    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->  "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH ->  "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
    }

}