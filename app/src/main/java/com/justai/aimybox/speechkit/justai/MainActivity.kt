package com.justai.aimybox.speechkit.justai

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.pytorch.demo.speechrecognition.R

class MainActivity : AppCompatActivity() {
    private lateinit var mTextView: TextView
    private lateinit var mButton: Button
    private lateinit var mButtonClear: Button

    companion object {
        private const val REQUEST_RECORD_AUDIO = 13
    }

    override fun onStop() {
        speechService.stop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        speechService.startListening(recognitionListener)
    }

    private val speechService by lazy {
        SpeechService(this)
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onResult(hypothesis: String?) {
            if (hypothesis != null)
                runOnUiThread {
                    mTextView.text = "${mTextView.text}$hypothesis"
                }
        }

        override fun onError(exception: Exception?) {
            Log.e("VoiceTrigger", "Error", exception)
        }

    }

    private var isButtonPressed = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mButton = findViewById(R.id.btnRecognize)
        mTextView = findViewById(R.id.tvResult)
        mButtonClear = findViewById(R.id.btnClear)

        mButtonClear.setOnClickListener {
            mTextView.text = ""
        }

        mButton.setOnClickListener {
            if (!isButtonPressed) {
                mButton.text = "Stop"
                speechService.startListening(recognitionListener)
            } else {
                mButton.text = "Start"
                speechService.stop()
            }
            isButtonPressed = !isButtonPressed
        }
        requestMicrophonePermission()
    }

    private fun requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        }
    }
}