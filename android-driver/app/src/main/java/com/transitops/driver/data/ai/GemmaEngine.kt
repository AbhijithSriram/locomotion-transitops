package com.transitops.driver.data.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object GemmaEngine {
    private const val TAG = "GemmaEngine"
    private var inference: LlmInference? = null
    var isReady = false
        private set

    // Expected path for the model pushed via ADB
    private const val MODEL_PATH = "/data/local/tmp/llm/gemma2b-it-q4.task"

    fun init(context: Context) {
        if (isReady) return

        val modelFile = File(MODEL_PATH)
        if (!modelFile.exists()) {
            Log.e(TAG, "Model not found at $MODEL_PATH. Push it via ADB: adb push model.task /data/local/tmp/llm/")
            return
        }

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(MODEL_PATH)
                .setMaxTokens(512)
                .build()
            inference = LlmInference.createFromOptions(context, options)
            isReady = true
            Log.i(TAG, "Gemma loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Gemma", e)
        }
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.Default) {
        if (!isReady || inference == null) {
            return@withContext "{\"error\": \"Model not loaded. Ensure the .task file is on the device at $MODEL_PATH.\"}"
        }
        try {
            inference!!.generateResponse(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            "{\"error\": \"Generation failed: ${e.message}\"}"
        }
    }
}
