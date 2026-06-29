package tech.phantom.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Phantom — the phone brain's front-end.
 *
 * You type a goal; the app runs the local `phantom` LAM in Termux, which sees
 * the PC screen and acts on it. Settings (PC gate URL, model) persist locally.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("phantom", Context.MODE_PRIVATE)

        val pcUrl = findViewById<EditText>(R.id.pcUrl)
        val model = findViewById<EditText>(R.id.model)
        val goal = findViewById<EditText>(R.id.goal)
        logView = findViewById(R.id.log)
        val runBtn = findViewById<Button>(R.id.runButton)

        // Restore saved settings.
        pcUrl.setText(prefs.getString("pc_url", "http://192.168.1.50:8765"))
        model.setText(prefs.getString("model", "qwen2.5-vl:7b"))

        runBtn.setOnClickListener {
            val url = pcUrl.text.toString().trim()
            val mdl = model.text.toString().trim()
            val g = goal.text.toString().trim()
            if (url.isEmpty() || g.isEmpty()) {
                appendLog("Enter the PC gate URL and a goal first.")
                return@setOnClickListener
            }
            prefs.edit().putString("pc_url", url).putString("model", mdl).apply()
            appendLog("▶ Running: $g")
            try {
                TermuxRunner.run(this, url, mdl, g, resultPendingIntent())
            } catch (e: Exception) {
                appendLog("Failed to reach Termux: ${e.message}\n" +
                    "Is Termux installed and 'allow-external-apps' enabled?")
            }
        }
    }

    /** PendingIntent the Termux service fills with stdout/stderr and broadcasts. */
    private fun resultPendingIntent(): PendingIntent {
        val intent = Intent(this, PhantomResultReceiver::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(this, 0, intent, flags)
    }

    private fun appendLog(line: String) {
        logView.append(if (logView.text.isEmpty()) line else "\n$line")
    }

    override fun onResume() {
        super.onResume()
        listener = { text -> appendLog(text) }
    }

    override fun onPause() {
        super.onPause()
        listener = null
    }

    companion object {
        private val main = Handler(Looper.getMainLooper())
        @Volatile private var listener: ((String) -> Unit)? = null

        /** Called from the result receiver; posts output to the UI thread. */
        fun deliver(text: String) {
            main.post { listener?.invoke(text) }
        }
    }
}
