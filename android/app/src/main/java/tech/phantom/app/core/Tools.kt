package tech.phantom.app.core

import org.json.JSONObject

/**
 * The brain's action space + parsing of the model's tool-calls. Kotlin port of
 * phantom/phone/tools.py — kept byte-for-byte compatible in behaviour.
 */
object Tools {

    const val TOOLS_DESCRIPTION = """You may use exactly one action per step. Reply with ONLY a JSON object:
{"thought": "<short reasoning>", "action": {"type": "<name>", ...}}

Actions that operate the computer:
- {"type": "click", "x": <int>, "y": <int>, "button": "left|right|middle", "clicks": <int>}
- {"type": "move", "x": <int>, "y": <int>}
- {"type": "type", "text": "<text to type>"}
- {"type": "key", "keys": ["ctrl", "c"]}
- {"type": "scroll", "dx": <int>, "dy": <int>}
- {"type": "wait", "seconds": <number>}

Control actions:
- {"type": "screenshot"}
- {"type": "done", "summary": "<what you accomplished>"}

Coordinates are pixels on the screenshot you are shown. Think step by step:
look at the screen, decide the single best next action toward the goal, and
emit it. When the goal is achieved, use "done"."""

    val CONTROL_ACTIONS = setOf("screenshot", "done")
    val EXECUTABLE_ACTIONS = setOf("click", "move", "type", "key", "scroll", "wait")

    /** Extract the JSON decision from raw model text (tolerates fences / prose). */
    fun parseDecision(text: String): JSONObject {
        val t = text.trim()

        val fenced = Regex("```(?:json)?\\s*(\\{.*?})\\s*```", RegexOption.DOT_MATCHES_ALL).find(t)
        if (fenced != null) return JSONObject(fenced.groupValues[1])

        val start = t.indexOf('{')
        require(start != -1) { "no JSON object found in model output" }

        var depth = 0
        for (i in start until t.length) {
            when (t[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return JSONObject(t.substring(start, i + 1))
                }
            }
        }
        error("unbalanced JSON object in model output")
    }
}
