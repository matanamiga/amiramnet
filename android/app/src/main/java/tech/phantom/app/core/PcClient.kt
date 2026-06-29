package tech.phantom.app.core

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * HTTP client for the PC gate. Kotlin port of phantom/phone/pc_client.py, using
 * the same wire contract (GET /screenshot, POST /action).
 */
class PcClient(baseUrl: String, private val client: OkHttpClient) {

    private val base = baseUrl.trimEnd('/')
    private val jsonMedia = "application/json".toMediaType()

    data class Shot(val imageB64: String, val width: Int, val height: Int)

    fun screenshot(scale: Double): Shot {
        val req = Request.Builder().url("$base/screenshot?scale=$scale").get().build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("gate /screenshot returned HTTP ${r.code}")
            val o = JSONObject(r.body?.string().orEmpty())
            return Shot(o.getString("image_b64"), o.getInt("width"), o.getInt("height"))
        }
    }

    /** POST an executable action (already a {"type":...} object) to the gate. */
    fun doAction(action: JSONObject): String {
        val body = JSONObject().put("action", action).toString().toRequestBody(jsonMedia)
        val req = Request.Builder().url("$base/action").post(body).build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("gate /action returned HTTP ${r.code}")
            val o = JSONObject(r.body?.string().orEmpty())
            val detail = o.optString("detail")
            val status = if (o.optBoolean("ok", false)) "ok" else "failed"
            return if (detail.isEmpty()) status else "$status: $detail"
        }
    }
}
