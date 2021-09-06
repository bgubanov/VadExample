package com.justai.aimybox.speechkit.justai

interface RecognitionListener {
    fun onResult(hypothesis: String?)

    fun onError(exception: Exception?)
}