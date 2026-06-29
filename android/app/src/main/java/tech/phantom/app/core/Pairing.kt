package tech.phantom.app.core

import android.util.Base64
import org.json.JSONObject

/** Decodes the `PHANTOM:<base64>` pairing code printed by the PC gate. */
object Pairing {

    data class Pair(val url: String, val token: String, val model: String)

    fun decode(code: String): Pair {
        var c = code.trim()
        if (c.startsWith("PHANTOM:")) c = c.substring("PHANTOM:".length)
        val raw = Base64.decode(c, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val o = JSONObject(String(raw, Charsets.UTF_8))
        val url = o.optString("url")
        val token = o.optString("token")
        require(url.isNotEmpty() && token.isNotEmpty()) { "pairing code missing url/token" }
        return Pair(url, token, o.optString("model"))
    }
}
