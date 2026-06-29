package tech.phantom.app.core

/**
 * Sliding-window memory: keeps only the last [window] steps so the prompt (and
 * phone RAM) stay bounded as a task runs long. Mirrors phantom/phone/memory.py.
 */
class SlidingMemory(private val window: Int = 4) {

    private data class Step(val thought: String, val action: String, val result: String)

    private val steps = ArrayDeque<Step>()

    fun add(thought: String, action: String, result: String) {
        steps.addLast(Step(thought, action, result))
        while (steps.size > window) steps.removeFirst()
    }

    fun render(): String {
        if (steps.isEmpty()) return "No actions yet."
        return steps.mapIndexed { i, s ->
            "${i + 1}. thought=${s.thought} | action=${s.action} -> ${s.result}"
        }.joinToString("\n")
    }
}
