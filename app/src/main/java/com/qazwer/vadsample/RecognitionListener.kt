package com.qazwer.vadsample

interface RecognitionListener {
    fun onResult(hypothesis: String?)

    fun onError(exception: Exception?)
}