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
import android.os.Handler
import android.os.Looper
import kotlin.concurrent.timerTask

private var isProcessingImage = false

@androidx.camera.core.ExperimentalGetImage
private fun barcodeDetect(context: Context, cameraExecutor: ExecutorService) {
    val barcodeScanner = BarcodeScanning.getClient()
    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
        if (!isProcessingImage) {
            isProcessingImage = true
            processImageProxy(context, barcodeScanner, imageProxy)
        } else {
            imageProxy.close()
        }
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
        var codeBar: String? = null
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (isProcessingImage) {
                imageProxy.close()
                isProcessingImage = false
                reloadCamera(context)
            }
        }
        handler.postDelayed(timeoutRunnable, 2000) // 2 seconds timeout

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    codeBar = barcode.rawValue
                }
            }
            .addOnFailureListener {
                reloadCamera(context)
            }
            .addOnCompleteListener {
                handler.removeCallbacks(timeoutRunnable)
                imageProxy.close()
                isProcessingImage = false
                codeBar?.let {
                    openScreen(context, it)
                } ?: reloadCamera(context)
            }
    } else {
        imageProxy.close()
        isProcessingImage = false
        reloadCamera(context)
    }
}

@OptIn(ExperimentalGetImage::class)
private fun reloadCamera(context: Context) {
    startActivity(context, Intent(context, MainActivity::class.java), null)
    Toast.makeText(context, "Failed to detect barcode", Toast.LENGTH_SHORT).show()
}

private var isScreenOpened = false

private fun openScreen(context: Context, codeBar: String) {
    if (!isScreenOpened) {
        isScreenOpened = true
        val intent = Intent(context, SecondActivity::class.java)
        intent.putExtra("codeBar", codeBar)
        startActivity(context, intent, null)
        if (context is MainActivity) {
            context.finish()
        }
    }
}
    private fun showBarcodeInputDialog(context: Context) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle("Enter Barcode")

    val input = EditText(context)
    builder.setView(input)

    builder.setPositiveButton("OK") { dialog, _ ->
        val barcode = input.text.toString()
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

        cameraPreview = findViewById(R.id.camera_preview)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        val scanButton = findViewById<Button>(R.id.button)
        scanButton.setOnClickListener {
            barcodeDetect(this, cameraExecutor)
        }

        val manualScanButton = findViewById<Button>(R.id.button1)
        manualScanButton.setOnClickListener {
            showBarcodeInputDialog(this)
        }
    }

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