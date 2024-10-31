package com.example.scanglucideaide

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 123
        var DISPLAYVALUE = ""
    }

    private lateinit var qrCodeValueTextView: TextView
    private lateinit var startScanButton: Button
    private lateinit var scanSurfaceView: SurfaceView
    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var cameraSource: CameraSource
    private lateinit var recPro: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        qrCodeValueTextView = findViewById(R.id.qr_code_value_tv)
        startScanButton = findViewById(R.id.start_scan_btn)
        scanSurfaceView = findViewById(R.id.camera_preview)
        recPro = findViewById(R.id.rec_btn)
        qrCodeValueTextView.paintFlags = qrCodeValueTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        initBarcodeDetector()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (cameraPermissionGranted(requestCode, grantResults)) {
            finish()
            startActivity(intent)
        } else {
            Toast.makeText(this, "Camera is mandatory to scan QR code", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun cameraPermissionGranted(requestCode: Int, grantResults: IntArray): Boolean {
        return requestCode == CAMERA_PERMISSION_REQUEST_CODE
                && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun initBarcodeDetector() {
        barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.ALL_FORMATS)
            .build()

        if (!barcodeDetector.isOperational) {
            Toast.makeText(this, "Barcode detector is not operational", Toast.LENGTH_SHORT).show()
            return
        }

        initCameraSource()
        initScanSurfaceView()

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems
                if (barcodes.isNotEmpty()) {
                    barcodes.forEach { _, barcode ->
                        if (barcode.displayValue.isNotEmpty()) {
                            runOnUiThread {
                                startScanButton.setOnClickListener {
                                    onQrCodeScanned(barcode.displayValue)
                                }
                                recPro.setOnClickListener {
                                    DISPLAYVALUE = ""
                                    recQrCodeScanned(barcode.displayValue)
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun recQrCodeScanned(displayValue: String) {
        DISPLAYVALUE = displayValue
        val intent = Intent(this, RecBasPro::class.java)
        startActivity(intent)
    }

    private fun onQrCodeScanned(displayValue: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("QR Code Scanned")
        builder.setMessage(displayValue)
        builder.setIcon(R.drawable.ic_baseline_qr_code_24)
        builder.setNeutralButton("copy") { dialog, _ ->
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.text = displayValue
            dialog.dismiss()
        }
        builder.setNegativeButton("open") { dialog, _ ->
            DISPLAYVALUE = displayValue
            val intent = Intent(this, viewVente::class.java)
            startActivity(intent)
            dialog.dismiss()
        }
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun initCameraSource() {
        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true)
            .build()
    }

    private fun initScanSurfaceView() {
        scanSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        cameraSource.start(scanSurfaceView.holder)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(android.Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE
                    )
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        DISPLAYVALUE = ""
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}