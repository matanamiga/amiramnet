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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import tech.phantom.app.core.Agent
import tech.phantom.app.core.OllamaClient
import tech.phantom.app.core.Pairing
import tech.phantom.app.core.PcClient
import java.util.concurrent.TimeUnit

/**
 * Phantom — the phone brain's front-end.
 *
 * Pair with the PC gate (scan its QR or paste its code), then run a goal either
 * via the Termux `phantom` command (Phase 3) or with the in-app Kotlin LAM loop
 * (Phase 4). Settings persist locally.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var logView: TextView
    private lateinit var pairingCode: EditText
    private lateinit var pcUrl: EditText
    private lateinit var model: EditText
    private lateinit var ollamaUrl: EditText

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)   // vision inference can be slow
            .build()
    }

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == RESULT_OK) {
                res.data?.getStringExtra(ScanActivity.EXTRA_CODE)?.let { code ->
                    pairingCode.setText(code)
                    applyPairing(code)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("phantom", Context.MODE_PRIVATE)

        pairingCode = findViewById(R.id.pairingCode)
        pcUrl = findViewById(R.id.pcUrl)
        model = findViewById(R.id.model)
        ollamaUrl = findViewById(R.id.ollamaUrl)
        val goal = findViewById<EditText>(R.id.goal)
        logView = findViewById(R.id.log)
        val scanBtn = findViewById<Button>(R.id.scanButton)
        val pairBtn = findViewById<Button>(R.id.pairButton)
        val runTermuxBtn = findViewById<Button>(R.id.runButton)
        val runOnDeviceBtn = findViewById<Button>(R.id.runOnDeviceButton)

        pcUrl.setText(prefs.getString("pc_url", "http://192.168.1.50:8765"))
        model.setText(prefs.getString("model", "qwen2.5-vl:7b"))
        ollamaUrl.setText(prefs.getString("ollama_url", "http://127.0.0.1:11434"))

        scanBtn.setOnClickListener { scanLauncher.launch(Intent(this, ScanActivity::class.java)) }
        pairBtn.setOnClickListener { applyPairing(pairingCode.text.toString()) }

        runTermuxBtn.setOnClickListener {
            val (url, mdl, g) = readInputs(goal) ?: return@setOnClickListener
            appendLog("▶ Termux: $g")
            try {
                TermuxRunner.run(this, url, mdl, token(), g, resultPendingIntent())
            } catch (e: Exception) {
                appendLog("Failed to reach Termux: ${e.message}\n" +
                    "Is Termux installed and 'allow-external-apps' enabled?")
            }
        }

        runOnDeviceBtn.setOnClickListener {
            val (url, mdl, g) = readInputs(goal) ?: return@setOnClickListener
            val ollama = ollamaUrl.text.toString().trim().ifEmpty { "http://127.0.0.1:11434" }
            appendLog("▶ On-device: $g")
            runOnDevice(url, ollama, mdl, g)
        }
    }

    /** Decode a PHANTOM: pairing code and fill URL + token (+ model). */
    private fun applyPairing(code: String) {
        try {
            val p = Pairing.decode(code)
            pcUrl.setText(p.url)
            if (p.model.isNotEmpty()) model.setText(p.model)
            prefs.edit().putString("token", p.token).apply()
            appendLog("✓ Paired with ${p.url}")
        } catch (e: Exception) {
            appendLog("Bad pairing code: ${e.message}")
        }
    }

    private fun readInputs(goal: EditText): Triple<String, String, String>? {
        val url = pcUrl.text.toString().trim()
        val mdl = model.text.toString().trim()
        val g = goal.text.toString().trim()
        if (url.isEmpty() || g.isEmpty()) {
            appendLog("Enter the PC gate URL and a goal first.")
            return null
        }
        prefs.edit()
            .putString("pc_url", url)
            .putString("model", mdl)
            .putString("ollama_url", ollamaUrl.text.toString().trim())
            .apply()
        return Triple(url, mdl, g)
    }

    private fun token(): String = prefs.getString("token", "").orEmpty()

    /** Run the Kotlin LAM loop off the UI thread, streaming progress to the log. */
    private fun runOnDevice(pcUrl: String, ollamaUrl: String, model: String, goal: String) {
        Thread {
            val agent = Agent(
                pc = PcClient(pcUrl, http, token()),
                ollama = OllamaClient(ollamaUrl, model, numThreads = 4, client = http),
            )
            val summary = agent.run(goal) { line -> runOnUiThread { appendLog(line) } }
            runOnUiThread { appendLog("— $summary —") }
        }.start()
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
