package com.justai.aimybox.speechkit.justai

import android.content.Context
import android.renderscript.Float4
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.exp
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class Recognizer(private val context: Context) {

    companion object {
        private const val VAD_TRESHOLD = 0.5f
    }

    private val vadModule: Module by lazy {
        loadModule("vad.jit").also {
            Log.d("PyTorch", "Vad module has been initialized")
        }
    }
    private val featureExtractorModule: Module by lazy {
        loadModule("feature_extractor.jit").also {
            Log.d("PyTorch", "FatureExtractor module has been initialized")
        }
    }
    private val recognizerModule: Module by lazy {
        loadModule("model.jit").also {
            Log.d("PyTorch", "Recognizer module has been initialized")
        }
    }

    @ExperimentalTime
    fun recognize(floatInputBuffer: FloatArray): Float {
        var startTime = System.currentTimeMillis()
        var resultFeatures: IValue
        val fTime = measureTime {
            resultFeatures = featureExtractorModule.getResult(floatInputBuffer)
        }
        Log.d("FeatureExtractor", "Start time: $startTime, bufferSize: ${floatInputBuffer.size}, duration: ${fTime}")
        startTime = System.currentTimeMillis()
        var result: FloatArray
        val rTime = measureTime {
            result = recognizerModule.forward(resultFeatures).toTensor().dataAsFloatArray
        }
        Log.d("Spotter", "Start time: $startTime, bufferSize: ${floatInputBuffer.size}, duration: ${rTime}")

        return softMax(result)
    }

    private fun softMax(result: FloatArray, index: Int = 1): Float {
        val sum = result.map { exp(it) }.sum()
        return (exp(result[index]) / sum)
    }

    fun checkIfChunkHasVoice(floatInputBuffer: FloatArray): Boolean {
        val result = vadModule.getResult(floatInputBuffer)
        val probabilityOfSpeech = result.toTensor().dataAsFloatArray[1]
        return probabilityOfSpeech > VAD_TRESHOLD
    }

    private fun loadModule(path: String): Module {
        val modulePath = assetFilePath(context, path)
        val moduleFileAbsoluteFilePath = File(modulePath).absolutePath
        return Module.load(moduleFileAbsoluteFilePath)
    }

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    private fun Module.getResult(floatInputBuffer: FloatArray): IValue {
        val inTensorBuffer = Tensor.allocateFloatBuffer(floatInputBuffer.size)
        inTensorBuffer.put(floatInputBuffer)
        val inTensor =
            Tensor.fromBlob(inTensorBuffer, longArrayOf(1, floatInputBuffer.size.toLong()))
        return forward(IValue.from(inTensor))
    }
}

