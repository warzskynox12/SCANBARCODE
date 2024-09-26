package com.example.scanglucideaide

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.camera.core.ExperimentalGetImage
private fun barcodeDetect(context: Context, cameraExecutor: ExecutorService) {
    val barcodeScanner = BarcodeScanning.getClient()
    // Set up the camera to detect barcodes
    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
        processImageProxy(context, barcodeScanner, imageProxy)
    }

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as LifecycleOwner, cameraSelector, imageAnalysis
            )
        } catch (exc: Exception) {
            Toast.makeText(context, "Failed to start camera", Toast.LENGTH_SHORT).show()
        }
    }, ContextCompat.getMainExecutor(context))
}

@androidx.camera.core.ExperimentalGetImage
private fun processImageProxy(context: Context, scanner: BarcodeScanner, imageProxy: ImageProxy) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    // Handle the detected barcode
                    val codeBar = barcode.rawValue
                    if (codeBar != null) openScreen(context, codeBar)
                }
            }
            .addOnFailureListener {
                // Handle failure
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
private fun openScreen(context: Context, codeBar: String) {
    val intent = Intent(context, SecondActivity::class.java)
    intent.putExtra("codeBar", codeBar)
    startActivity(context, intent, null)
}

private fun showBarcodeInputDialog(context: Context) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle("Enter Barcode")

    // Set up the input
    val input = EditText(context)
    builder.setView(input)

    // Set up the buttons
    builder.setPositiveButton("OK") { dialog, _ ->
        val barcode = input.text.toString()
        // Handle the barcode input
        Toast.makeText(context, "Barcode entered: $barcode", Toast.LENGTH_SHORT).show()
        dialog.dismiss()
    }
    builder.setNegativeButton("Cancel") { dialog, _ ->
        dialog.cancel()
    }

    builder.show()
}
class MainActivity : AppCompatActivity() {

    private lateinit var cameraPreview: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null

    // Permission request for CameraX
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Camera preview
        cameraPreview = findViewById(R.id.camera_preview)

        // Initialize camera executor for background tasks
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check camera permission and start camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Button for automatic scan
        val scanButton = findViewById<Button>(R.id.button)
        scanButton.setOnClickListener {
            barcodeDetect(this, cameraExecutor)
        }

        // Button for manual scan
        val manualScanButton = findViewById<Button>(R.id.button1)
        manualScanButton.setOnClickListener {
            showBarcodeInputDialog(this)
        }
    }

    // Start camera using CameraX
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreview.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}