package com.example.jetpackcameraxapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.content.ContextCompat

fun Context.initializeSpeechRecognizer(): SpeechRecognizer {
    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    recognizerIntent.putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
    )
    recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)

    val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {}

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                processVoiceCommand(matches[0])
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    speechRecognizer.setRecognitionListener(listener)

    val permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
        speechRecognizer.startListening(recognizerIntent)
    } else {
        Toast.makeText(
            this,
            "Please grant the microphone permission to use voice commands.",
            Toast.LENGTH_SHORT
        ).show()
    }

    return speechRecognizer
}

fun Context.processVoiceCommand(command: String) {
    if (command.toLowerCase() == "open second camera") {
        Toast.makeText(this, "Opening CameraXApp", Toast.LENGTH_SHORT).show()
        val intent = packageManager.getLaunchIntentForPackage("com.android.example.jetpackcameraxapp")
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "CameraXApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }
}

