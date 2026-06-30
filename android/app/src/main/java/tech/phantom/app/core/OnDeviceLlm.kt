package tech.phantom.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession

/**
 * Embedded vision LLM via MediaPipe LLM Inference — runs a local multimodal
 * model (e.g. Gemma 3n) entirely inside the app. No Ollama, no Termux.
 *
 * The model is a `.task` file the user downloads once (see [ModelManager]); its
 * absolute path is passed here. Inference is slow on a phone — the same
 * constraint as Ollama, since both are on-device.
 */
class OnDeviceLlm(
    context: Context,
    modelPath: String,
    maxTokens: Int = 1024,
) : VisionLlm {

    private val engine: LlmInference = LlmInference.createFromOptions(
        context,
        LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(maxTokens)
            .setMaxNumImages(1)
            .build(),
    )

    override fun generate(system: String, prompt: String, imageB64: String): String {
        val session = LlmInferenceSession.createFromOptions(
            engine,
            LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(40)
                .setTemperature(0.6f)
                .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
                .build(),
        )
        session.use { s ->
            s.addQueryChunk("$system\n\n$prompt")
            decodeBitmap(imageB64)?.let { bmp ->
                s.addImage(BitmapImageBuilder(bmp).build())
            }
            return s.generateResponse()
        }
    }

    fun close() = engine.close()

    private fun decodeBitmap(b64: String): Bitmap? = try {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
