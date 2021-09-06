# Porting VAD to Android application

## Setting up

### 1. Setting up PyTorch Android dependency

Insert new line to app level build.gradle file:

```kotlin
 implementation("org.pytorch:pytorch_android:1.9.0")
```

### 2. Downloading model

Get model and move it to src/main/assets directory (`vad.jit` in this example)

## Usage

### Initializing model

To use model we must first initialize Model object of our model

To do it I wrote 2 useful functions

```kotlin
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
```

Usage:

```kotlin
private val vadModule: Module by lazy {
    loadModule("vad.jit").also {
        Log.d("PyTorch", "Vad module has been initialized")
    }
}
```

### Getting result from model

To get result from initialized model we can use getResult() function:

```kotlin
private fun Module.getResult(floatInputBuffer: FloatArray): IValue {
    val inTensorBuffer = Tensor.allocateFloatBuffer(floatInputBuffer.size)
    inTensorBuffer.put(floatInputBuffer)
    val inTensor =
        Tensor.fromBlob(inTensorBuffer, longArrayOf(1, floatInputBuffer.size.toLong()))
    return forward(IValue.from(inTensor))
}
```

Usage:

```kotlin
vadModule.getResult(audioInFloatArray)
```
where `audioInFloatArray` is a chunk of audio.
### Getting probability of speech

In our case, model returns an array `[a, b]`, where `a` is probability of not detecting speech 
and `b` is probability of detection speech. So we need to get the second element of this array.
To do this we can write:

```kotlin
val result = vadModule.getResult(audioInFloatArray)
val probabilityOfSpeech = result.toTensor().dataAsFloatArray[1]
```

## Example

You can get source code of sample app [here](https://github.com/bgubanov/VadExample).

Or you can download app from [here](https://github.com/bgubanov/VadExample/blob/demo-vad/demo-vad.apk).
