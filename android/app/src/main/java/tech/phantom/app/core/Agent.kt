package tech.phantom.app.core

/**
 * The on-device LAM loop. Kotlin port of phantom/phone/agent.py.
 *
 * observe (PC screenshot) -> decide (local Ollama vision) -> act (PC gate),
 * with bounded sliding-window memory, until the model says "done" or the step
 * cap is hit. Self-corrects on errors instead of crashing.
 */
class Agent(
    private val pc: PcClient,
    private val llm: VisionLlm,
    private val maxSteps: Int = 20,
    private val scale: Double = 0.75,
    memoryWindow: Int = 4,
) {
    private val memory = SlidingMemory(memoryWindow)
    private val system =
        "You are PHANTOM, a Large Action Model that operates a computer by sight.\n\n" +
            Tools.TOOLS_DESCRIPTION

    /**
     * Run the loop. [isCancelled] is checked each step for cooperative stop;
     * [log] receives human-readable progress lines.
     */
    fun run(goal: String, isCancelled: () -> Boolean = { false }, log: (String) -> Unit): String {
        for (step in 1..maxSteps) {
            if (isCancelled()) {
                log("■ stopped")
                return "Stopped by user."
            }
            try {
                log("step $step · looking at the screen…")
                val shot = pc.screenshot(scale)
                val prompt = buildString {
                    append("Goal: ").append(goal).append("\n\n")
                    append("Recent steps:\n").append(memory.render()).append("\n\n")
                    append("Here is the current screen (")
                    append(shot.width).append("x").append(shot.height).append(").")
                }
                val text = llm.generate(system, prompt, shot.imageB64)
                val decision = Tools.parseDecision(text)
                val thought = decision.optString("thought")
                val action = decision.getJSONObject("action")
                val type = action.optString("type")
                log("step $step · $thought  [$type]")

                when (type) {
                    "done" -> {
                        val summary = action.optString("summary", "done")
                        log("✓ $summary")
                        return summary
                    }
                    "screenshot" -> memory.add(thought, type, "re-observed")
                    in Tools.EXECUTABLE_ACTIONS -> {
                        val result = pc.doAction(action)
                        memory.add(thought, type, result)
                        log("   → $result")
                    }
                    else -> {
                        memory.add(thought, type, "unknown action")
                        log("   ! unknown action: $type")
                    }
                }
            } catch (e: Exception) {
                log("step $step · error: ${e.message}")
                memory.add("", "error", e.message ?: "error")
            }
        }
        return "Stopped after $maxSteps steps."
    }
}
