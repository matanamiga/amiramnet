package tech.phantom.app.core

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Manages the on-device model file (a MediaPipe `.task`). The model is large
 * (GBs) so it is downloaded once into app storage rather than bundled in the
 * APK. Provide a direct download URL (some models require accepting a license
 * and using an authenticated link).
 */
class ModelManager(private val context: Context, private val http: OkHttpClient) {

    fun modelFile(): File = File(context.filesDir, "phantom-model.task")

    fun isPresent(): Boolean = modelFile().let { it.exists() && it.length() > 0 }

    /** Download the model, reporting fractional progress in [0,1]. Atomic via a .part file. */
    fun download(url: String, onProgress: (Float) -> Unit) {
        val req = Request.Builder().url(url).build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("model download returned HTTP ${r.code}")
            val body = r.body ?: error("empty response body")
            val total = body.contentLength()
            val part = File(context.filesDir, "phantom-model.part")
            body.byteStream().use { input ->
                part.outputStream().use { out ->
                    val buf = ByteArray(1 shl 16)
                    var done = 0L
                    var read = input.read(buf)
                    while (read >= 0) {
                        out.write(buf, 0, read)
                        done += read
                        if (total > 0) onProgress(done.toFloat() / total)
                        read = input.read(buf)
                    }
                }
            }
            if (!part.renameTo(modelFile())) error("could not finalize model file")
        }
    }
}
