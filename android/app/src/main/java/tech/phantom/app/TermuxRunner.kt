package tech.phantom.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Bridges the Phantom UI to the `phantom` command installed in Termux.
 *
 * Phase 3 keeps all the agent intelligence in the existing Python brain; this
 * app is a friendly front-end that asks Termux to run it. (Phase 4 ports the
 * loop into the APK itself.)
 *
 * Requires Termux's "allow-external-apps = true" property and granting the
 * RUN_COMMAND permission — see docs/ANDROID.md.
 */
object TermuxRunner {

    private const val TERMUX_PKG = "com.termux"
    private const val RUN_SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION_RUN = "com.termux.RUN_COMMAND"
    private const val BASH = "/data/data/com.termux/files/usr/bin/bash"
    private const val HOME = "/data/data/com.termux/files/home"

    /** Launch `phantom "<goal>"` in Termux with the configured PC URL / model. */
    fun run(
        context: Context,
        pcUrl: String,
        model: String,
        token: String,
        goal: String,
        result: PendingIntent,
    ) {
        // Escape double quotes so the goal survives the shell.
        val safeGoal = goal.replace("\"", "\\\"")
        val script =
            "PHANTOM_PC_URL='$pcUrl' PHANTOM_MODEL='$model' PHANTOM_TOKEN='$token' phantom \"$safeGoal\""

        val intent = Intent().apply {
            setClassName(TERMUX_PKG, RUN_SERVICE)
            action = ACTION_RUN
            putExtra("com.termux.RUN_COMMAND_PATH", BASH)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", script))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", result)
        }
        ContextCompat.startForegroundService(context, intent)
    }
}
