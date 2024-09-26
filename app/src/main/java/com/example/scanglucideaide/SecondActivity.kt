// Ensure SecondActivity extends AppCompatActivity or Activity
package com.example.scanglucideaide

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        val codeBar = intent.getStringExtra("codeBar")
    }
}