// Ensure SecondActivity extends AppCompatActivity or Activity
package com.example.scanglucideaide

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat

class SecondActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        val codeBar = intent.getStringExtra("codeBar")
        val textView = findViewById<TextView>(R.id.textView)
        textView.text = codeBar
        val manualScanButton = findViewById<Button>(R.id.button_return)
        manualScanButton.setOnClickListener {
            returnScan(this)
        }
    }

    private fun returnScan(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        ContextCompat.startActivity(context, intent, null)
    }

}