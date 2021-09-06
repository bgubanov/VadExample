package com.justai.aimybox.speechkit.justai

import android.content.Context
import com.justai.aimybox.voicetrigger.VoiceTrigger

class JustAiVoiceTrigger(context: Context) : VoiceTrigger {
    private val speechService by lazy { SpeechService(context) }

    private inline fun recognitionListener(
        crossinline onTriggered: (String?) -> Unit,
        crossinline onError: (e: Throwable) -> Unit): RecognitionListener {
            return object : RecognitionListener {
                override fun onResult(hypothesis: String?) = onTriggered(hypothesis)
                override fun onError(exception: Exception?) = exception?.let { onError(it) } ?: Unit
            }
        }

    override suspend fun startDetection(
        onTriggered: (phrase: String?) -> Unit,
        onException: (e: Throwable) -> Unit
    ) {
        speechService.startListening(recognitionListener(onTriggered, onException))
    }

    override suspend fun stopDetection() {
        speechService.stop()
    }

    override fun destroy() {
        speechService.stop()
        speechService.shutdown()
        super.destroy()
    }
}