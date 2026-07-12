package com.transitops.driver.data.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.nehuatl.llamacpp.LlamaHelper
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object GemmaEngine {
    private const val TAG = "GemmaEngine"
    private var llamaHelper: LlamaHelper? = null
    var isReady = false
        private set

    // Expected path for the Qwen/Phi .gguf model (will be resolved at runtime)
    private var modelPath: String? = null

    private val _llmFlow = MutableSharedFlow<LlamaHelper.LLMEvent>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun init(context: Context) {
        if (isReady) return
        
        // Target path in internal storage
        val internalPath = context.filesDir.absolutePath + "/model.gguf"
        val internalFile = File(internalPath)
        
        if (!internalFile.exists()) {
            Log.d(TAG, "Model not found in internal storage. Extracting from assets...")
            try {
                withContext(Dispatchers.IO) {
                    context.assets.open("model.gguf").use { input ->
                        internalFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                Log.d(TAG, "Model extraction complete.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract model from assets", e)
                return
            }
        }
        
        modelPath = android.net.Uri.fromFile(internalFile).toString()

        try {
            llamaHelper = LlamaHelper(
                contentResolver = context.contentResolver,
                scope = scope,
                sharedFlow = _llmFlow
            )
            
            // Suspend until loaded
            suspendCoroutine<Unit> { continuation ->
                llamaHelper?.load(
                    path = modelPath!!,
                    contextLength = 1024,
                    mmprojPath = null
                ) {
                    isReady = true
                    Log.i(TAG, "Llama.cpp model loaded successfully")
                    continuation.resume(Unit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
        }
    }

    suspend fun generate(prompt: String): String = suspendCoroutine { continuation ->
        if (!isReady || llamaHelper == null) {
            continuation.resume("{\"error\": \"Model not loaded. Please copy model.gguf into Android/data/com.transitops.driver/files/ via USB.\"}")
            return@suspendCoroutine
        }
        
        var generatedText = ""
        var isDone = false
        
        // Launch a one-off collector for this generation
        val collectJob = scope.launch {
            _llmFlow.collect { event ->
                if (isDone) return@collect
                
                when (event) {
                    is LlamaHelper.LLMEvent.Ongoing -> {
                        generatedText += event.word
                    }
                    is LlamaHelper.LLMEvent.Done -> {
                        isDone = true
                        llamaHelper?.stopPrediction()
                        continuation.resume(generatedText)
                    }
                    is LlamaHelper.LLMEvent.Error -> {
                        isDone = true
                        llamaHelper?.stopPrediction()
                        continuation.resume("{\"error\": \"Generation failed: ${event.message}\"}")
                    }
                    else -> {}
                }
            }
        }
        
        try {
            llamaHelper?.predict(prompt, null)
        } catch (e: Exception) {
            if (!isDone) {
                isDone = true
                collectJob.cancel()
                continuation.resume("{\"error\": \"Prediction exception: ${e.message}\"}")
            }
        }
    }
}
