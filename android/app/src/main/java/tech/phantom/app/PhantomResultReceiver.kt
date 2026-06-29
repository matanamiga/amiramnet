package tech.phantom.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Receives the stdout / stderr / exit code that the Termux RUN_COMMAND service
 * sends back through our PendingIntent, and forwards it to the UI.
 */
class PhantomResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result: Bundle? = intent.getBundleExtra("result")
        val stdout = result?.getString("stdout").orEmpty()
        val stderr = result?.getString("stderr").orEmpty()
        val exit = result?.getInt("exitCode", -1) ?: -1

        val text = buildString {
            if (stdout.isNotBlank()) append(stdout.trim())
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append("[stderr] ").append(stderr.trim())
            }
            if (isEmpty()) append("(no output)")
            append("\n— finished (exit ").append(exit).append(") —")
        }
        MainActivity.deliver(text)
    }
}
