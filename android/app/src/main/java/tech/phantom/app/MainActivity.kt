package tech.phantom.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import tech.phantom.app.core.GateDiscovery
import tech.phantom.app.core.ModelManager
import tech.phantom.app.core.Pairing
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
    private lateinit var modelUrl: EditText
    private lateinit var useOnDevice: CheckBox

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
        modelUrl = findViewById(R.id.modelUrl)
        useOnDevice = findViewById(R.id.useOnDevice)
        val goal = findViewById<EditText>(R.id.goal)
        logView = findViewById(R.id.log)
        val downloadBtn = findViewById<Button>(R.id.downloadModelButton)
        val discoverBtn = findViewById<Button>(R.id.discoverButton)
        val scanBtn = findViewById<Button>(R.id.scanButton)
        val pairBtn = findViewById<Button>(R.id.pairButton)
        val runTermuxBtn = findViewById<Button>(R.id.runButton)
        val runOnDeviceBtn = findViewById<Button>(R.id.runOnDeviceButton)
        val stopBtn = findViewById<Button>(R.id.stopButton)

        pcUrl.setText(prefs.getString("pc_url", "http://192.168.1.50:8765"))
        model.setText(prefs.getString("model", "qwen2.5-vl:7b"))
        ollamaUrl.setText(prefs.getString("ollama_url", "http://127.0.0.1:11434"))
        modelUrl.setText(prefs.getString("model_url", ""))
        useOnDevice.isChecked = prefs.getBoolean("use_on_device", false)

        downloadBtn.setOnClickListener { downloadModel(modelUrl.text.toString().trim()) }
        discoverBtn.setOnClickListener { startDiscovery() }
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

        stopBtn.setOnClickListener {
            startService(Intent(this, PhantomService::class.java).setAction(PhantomService.ACTION_STOP))
            appendLog("■ Stopping…")
        }
    }

    private var discovery: GateDiscovery? = null

    /** Find a gate on the LAN via mDNS and auto-fill URL + token. */
    private fun startDiscovery() {
        discovery?.stop()
        appendLog("⌕ Searching the Wi-Fi for a gate…")
        discovery = GateDiscovery(this).also { d ->
            d.start(
                onFound = { found ->
                    runOnUiThread {
                        pcUrl.setText(found.url)
                        prefs.edit().putString("token", found.token).apply()
                        appendLog("✓ Found gate at ${found.url}")
                    }
                },
                onError = { msg -> runOnUiThread { appendLog("Discovery: $msg") } },
            )
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

    /** Run the LAM loop in a foreground service so it survives backgrounding. */
    private fun runOnDevice(pcUrl: String, ollamaUrl: String, model: String, goal: String) {
        prefs.edit().putBoolean("use_on_device", useOnDevice.isChecked).apply()
        val engine = if (useOnDevice.isChecked) "ondevice" else "ollama"
        val intent = Intent(this, PhantomService::class.java)
            .putExtra(PhantomService.EXTRA_PC_URL, pcUrl)
            .putExtra(PhantomService.EXTRA_OLLAMA_URL, ollamaUrl)
            .putExtra(PhantomService.EXTRA_MODEL, model)
            .putExtra(PhantomService.EXTRA_TOKEN, token())
            .putExtra(PhantomService.EXTRA_GOAL, goal)
            .putExtra(PhantomService.EXTRA_ENGINE, engine)
        ContextCompat.startForegroundService(this, intent)
    }

    /** Download the on-device model once (large; runs off the UI thread). */
    private fun downloadModel(url: String) {
        if (url.isEmpty()) {
            appendLog("Enter a model URL (.task) first.")
            return
        }
        prefs.edit().putString("model_url", url).apply()
        appendLog("⤓ Downloading model… (this is large, be patient)")
        Thread {
            val http = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)   // no read timeout for a big download
                .build()
            try {
                var lastPct = -1
                ModelManager(applicationContext, http).download(url) { frac ->
                    val pct = (frac * 100).toInt()
                    if (pct != lastPct && pct % 5 == 0) {
                        lastPct = pct
                        runOnUiThread { appendLog("  model: $pct%") }
                    }
                }
                runOnUiThread { appendLog("✓ Model downloaded. Tick 'on-device' and Run.") }
            } catch (e: Exception) {
                runOnUiThread { appendLog("Model download failed: ${e.message}") }
            }
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
        discovery?.stop()
        discovery = null
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
