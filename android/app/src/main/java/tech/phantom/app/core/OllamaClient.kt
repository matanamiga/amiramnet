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
) {
    private val base = ollamaUrl.trimEnd('/')
    private val jsonMedia = "application/json".toMediaType()

    fun generate(system: String, prompt: String, imagesB64: List<String>): String {
        val payload = JSONObject()
            .put("model", model)
            .put("system", system)
            .put("prompt", prompt)
            .put("stream", false)
            // Cap CPU threads so the phone stays responsive while thinking.
            .put("options", JSONObject().put("num_thread", numThreads))
        if (imagesB64.isNotEmpty()) {
            val arr = JSONArray()
            imagesB64.forEach { arr.put(it) }
            payload.put("images", arr)
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
