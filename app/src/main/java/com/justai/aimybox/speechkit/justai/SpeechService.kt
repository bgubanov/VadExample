package com.justai.aimybox.speechkit.justai


import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import kotlin.time.ExperimentalTime

class SpeechService(
    context: Context,
    sampleRate: Int = 16000,
    val minPredictionSamples: Int = 3,
    val maxPredictionSamples: Int = 4,
    private val bufferSize: Int = 4000,
    private val paddingSize: Int = 3
) {

    companion object {
        const val RECOGNIZER_TRESHOLD = 0.6f
    }

    private val recognizer: Recognizer = Recognizer(context)

    private val recorder: AudioRecord = AudioRecord(
        AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2
    )

    private var recognizerThread: RecognizerThread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var listChunks = mutableListOf<FloatArray>()

    init {
        if (recorder.state == AudioRecord.STATE_UNINITIALIZED) {
            recorder.release()
            throw IOException(
                "Failed to initialize recorder. Microphone might be already in use."
            )
        }
    }

    fun startListening(listener: RecognitionListener): Boolean {
        if (recognizerThread != null) return false
        recognizerThread = RecognizerThread(listener)
        recognizerThread!!.start()
        Log.d("SpeechService", "Recognition has been started")
        return true
    }

    private fun stopRecognizerThread(): Boolean {
        if (recognizerThread == null) return false
        try {
            recognizerThread!!.interrupt()
            recognizerThread!!.join()
        } catch (e: InterruptedException) {
            // Restore the interrupted status.
            Thread.currentThread().interrupt()
        }
        recognizerThread = null
        Log.d("SpeechService", "Recognition has been stopped")
        return true
    }

    fun stop(): Boolean {
        return stopRecognizerThread()
    }

    fun cancel(): Boolean {
        recognizerThread?.setPause(true)
        return stopRecognizerThread()
    }

    fun shutdown() {
        recorder.stop()
        recorder.release()
    }

    fun setPause(paused: Boolean) {
        recognizerThread?.setPause(paused)
    }

    private inner class RecognizerThread(
        var listener: RecognitionListener
    ) : Thread() {
        @Volatile
        private var paused = false

        fun setPause(paused: Boolean) {
            this.paused = paused
        }

        @ExperimentalTime
        override fun run() {
            recorder.startRecording()
            if (recorder.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
                recorder.stop()
                val ioe = IOException(
                    "Failed to start recording. Microphone might be already in use."
                )
                mainHandler.post { listener.onError(ioe) }
            }
            val buffer = ShortArray(bufferSize)
            while (!interrupted()) {
                val nread = recorder.read(buffer, 0, buffer.size)
                if (paused) continue
                if (nread < 0) throw RuntimeException("Error reading audio buffer")
                val floatArray =
                    buffer.map { it.toFloat() / Short.MAX_VALUE.toFloat() }.toFloatArray()
                listChunks.add(floatArray)
                when {
                    !recognizer.checkIfChunkHasVoice(floatArray) -> {
                        listChunks = listChunks.takeLast(paddingSize).toMutableList()
                    }
                    listChunks.size > maxPredictionSamples -> {
                        listChunks.removeAt(0)
                    }
                    listChunks.size > minPredictionSamples -> {
                        val resultFloatArray = listChunks.reduce { one, another -> one + another }
                        val result = recognizer.recognize(resultFloatArray)
                        val text = if (result > RECOGNIZER_TRESHOLD) {
                            listChunks.clear()
                            "1"
                        } else "0"
                        mainHandler.post { listener.onResult(text) }
                    }
                }
            }
            recorder.stop()
        }
    }

}