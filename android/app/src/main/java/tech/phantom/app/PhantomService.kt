package tech.phantom.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import tech.phantom.app.core.Agent
import tech.phantom.app.core.OllamaClient
import tech.phantom.app.core.PcClient
import java.util.concurrent.TimeUnit

/**
 * Runs the on-device LAM loop as a foreground service so it keeps going when
 * the app is backgrounded, shows progress in a notification, and can be
 * stopped. Log lines are also forwarded to the UI via [MainActivity.deliver].
 */
class PhantomService : Service() {

    @Volatile private var cancelled = false
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            cancelled = true
            return START_NOT_STICKY
        }

        val pcUrl = intent?.getStringExtra(EXTRA_PC_URL).orEmpty()
        val ollamaUrl = intent?.getStringExtra(EXTRA_OLLAMA_URL).orEmpty()
        val model = intent?.getStringExtra(EXTRA_MODEL).orEmpty()
        val token = intent?.getStringExtra(EXTRA_TOKEN).orEmpty()
        val goal = intent?.getStringExtra(EXTRA_GOAL).orEmpty()

        startForeground(NOTIF_ID, notification("Running: $goal"))

        worker = Thread {
            val http = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build()
            val agent = Agent(
                pc = PcClient(pcUrl, http, token),
                ollama = OllamaClient(ollamaUrl, model, numThreads = 4, client = http),
            )
            val summary = agent.run(goal, isCancelled = { cancelled }) { line ->
                MainActivity.deliver(line)
                updateNotification(line)
            }
            MainActivity.deliver("— $summary —")
            stopForegroundCompat()
            stopSelf()
        }.also { it.start() }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        cancelled = true
        super.onDestroy()
    }

    private fun notification(text: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Phantom runs", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        val stopIntent = Intent(this, PhantomService::class.java).setAction(ACTION_STOP)
        var pflags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) pflags = pflags or PendingIntent.FLAG_IMMUTABLE
        val stop = PendingIntent.getService(this, 0, stopIntent, pflags)
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Phantom")
            .setContentText(text.take(120))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
            .build()
    }

    private fun updateNotification(text: String) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIF_ID, notification(text))
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    companion object {
        const val ACTION_STOP = "tech.phantom.app.STOP"
        const val EXTRA_PC_URL = "pc_url"
        const val EXTRA_OLLAMA_URL = "ollama_url"
        const val EXTRA_MODEL = "model"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_GOAL = "goal"
        private const val CHANNEL = "phantom_runs"
        private const val NOTIF_ID = 1
    }
}
