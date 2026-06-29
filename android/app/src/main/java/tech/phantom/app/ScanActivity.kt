package tech.phantom.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Scans the gate's pairing QR with the camera and returns its text to
 * [MainActivity] via the activity result (extra [EXTRA_CODE]).
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var handled = false

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else finishWith(null, "Camera permission denied")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        previewView = findViewById(R.id.previewView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, QrAnalyzer()) }

            provider.unbindAll()
            provider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private inner class QrAnalyzer : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient()

        @ExperimentalGetImage
        override fun analyze(image: androidx.camera.core.ImageProxy) {
            val media = image.image
            if (media == null || handled) {
                image.close()
                return
            }
            val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
            scanner.process(input)
                .addOnSuccessListener { codes ->
                    val raw = codes.firstOrNull { it.valueType == Barcode.TYPE_TEXT || it.rawValue != null }
                        ?.rawValue
                    if (!raw.isNullOrEmpty() && !handled) {
                        handled = true
                        finishWith(raw, null)
                    }
                }
                .addOnCompleteListener { image.close() }
        }
    }

    private fun finishWith(code: String?, error: String?) {
        if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        if (code != null) {
            setResult(RESULT_OK, intent.putExtra(EXTRA_CODE, code))
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        analysisExecutor.shutdown()
    }

    companion object {
        const val EXTRA_CODE = "pairing_code"
    }
}
