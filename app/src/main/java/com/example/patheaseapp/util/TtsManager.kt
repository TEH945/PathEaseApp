package com.example.patheaseapp.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Manages Text-to-Speech functionality for the app, providing a simple interface
 * to speak messages and handle initialization/shutdown lifecycle.
 */
class TtsManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TtsManager", "Language not supported")
                } else {
                    isInitialized = true
                }
            } else {
                Log.e("TtsManager", "Initialization failed")
            }
        }
    }

    /**
     * Speaks the given [text] using the configured TTS engine.
     */
    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.w("TtsManager", "TtsManager not yet initialized; message skipped: $text")
        }
    }

    /**
     * Stops any current speech and releases TTS resources.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
    }
}
