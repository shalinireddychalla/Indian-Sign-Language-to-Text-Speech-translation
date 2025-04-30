package com.example.myapplication

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * TextToSpeechManager is a utility class to handle text-to-speech functionality
 * in the sign language converter application.
 */
class TextToSpeechManager(
    private val context: Context
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TextToSpeechManager"
    }

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported")
            } else {
                isInitialized = true
                Log.d(TAG, "TextToSpeech initialized successfully")
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed")
        }
    }

    /**
     * Speaks the provided text
     * @param text The text to be converted to speech
     * @param queueMode How to handle existing speech requests (QUEUE_FLUSH or QUEUE_ADD)
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isInitialized && text.isNotEmpty()) {
            textToSpeech?.speak(text, queueMode, null, null)
        } else if (!isInitialized) {
            Log.e(TAG, "TextToSpeech not initialized")
        }
    }

    /**
     * Stop any current speech
     */
    fun stop() {
        if (isInitialized) {
            textToSpeech?.stop()
        }
    }

    /**
     * Release resources when no longer needed
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
}