package tech.phantom.app.core

/**
 * A vision-capable reasoning engine: given a system prompt, a user prompt and
 * one screenshot (base64 PNG), return the model's text.
 *
 * Implemented by [OllamaClient] (remote/Termux) and [OnDeviceLlm] (embedded
 * MediaPipe model — no Ollama, no Termux).
 */
interface VisionLlm {
    fun generate(system: String, prompt: String, imageB64: String): String
}
