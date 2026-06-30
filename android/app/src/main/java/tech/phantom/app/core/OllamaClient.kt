package tech.phantom.app.core

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local Ollama vision client. Kotlin port of phantom/llm/ollama_provider.py —
 * talks to the Ollama server running on the phone itself (in Termux).
 */
class OllamaClient(
    ollamaUrl: String,
    private val model: String,
    private val numThreads: Int,
    private val client: OkHttpClient,
) : VisionLlm {
    private val base = ollamaUrl.trimEnd('/')
    private val jsonMedia = "application/json".toMediaType()

    override fun generate(system: String, prompt: String, imageB64: String): String {
        val payload = JSONObject()
            .put("model", model)
            .put("system", system)
            .put("prompt", prompt)
            .put("stream", false)
            // Cap CPU threads so the phone stays responsive while thinking.
            .put("options", JSONObject().put("num_thread", numThreads))
        if (imageB64.isNotEmpty()) {
            payload.put("images", JSONArray().put(imageB64))
        }
        val req = Request.Builder()
            .url("$base/api/generate")
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("ollama returned HTTP ${r.code}")
            return JSONObject(r.body?.string().orEmpty()).optString("response")
        }
    }
}
