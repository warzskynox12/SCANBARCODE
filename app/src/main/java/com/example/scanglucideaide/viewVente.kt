package com.example.scanglucideaide

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.text.SpannableString
import android.text.Spanned

class viewVente : AppCompatActivity() {
    private lateinit var nameTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var codeTextView: TextView
    private lateinit var locTextView: TextView
    private lateinit var priceTextView: TextView
    private lateinit var statueTextView: TextView
    private lateinit var venduButton: Button
    private lateinit var returnButton: Button
    private val db = Firebase.firestore

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_vente)

        nameTextView = findViewById(R.id.name)
        imageView = findViewById(R.id.image)
        codeTextView = findViewById(R.id.code)
        locTextView = findViewById(R.id.loc)
        priceTextView = findViewById(R.id.price)
        statueTextView = findViewById(R.id.statue)
        venduButton = findViewById(R.id.vendu)
        returnButton = findViewById(R.id.buttonReturn)

        initLookData()
        initVendu()
        initReturn()
    }

    private fun initReturn() {
        returnButton.setOnClickListener {
            MainActivity.DISPLAYVALUE = ""
            finish()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initVendu() {
        venduButton.setOnClickListener {
            db.collection("products")
                .document(MainActivity.DISPLAYVALUE)
                .update("statue", false)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully updated!")
                    initStatueVente(false)
                }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
        }
    }

    private fun initLookData() {
        val code = MainActivity.DISPLAYVALUE
        db.collection("products")
            .document(code)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "DocumentSnapshot data: ${result.data}")
                nameTextView.text = result.data?.get("name").toString().uppercase()
                nameTextView.paintFlags = nameTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                codeTextView.text = result.data?.get("code").toString()
                codeTextView.paintFlags = codeTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                locTextView.text = result.data?.get("loc").toString()
                locTextView.paintFlags = locTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                priceTextView.text = result.data?.get("price").toString()
                priceTextView.paintFlags = priceTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG



                val imageString = result.data?.get("photo").toString()
                Glide.with(this)
                    .load(imageString)
                    .placeholder(R.drawable.ic_launcher_background) // Set a placeholder image
                    .error(R.drawable.ic_launcher_background) // Set an error image
                    .into(imageView)


                val statue = result.data?.get("statue") as? Boolean ?: false
                initStatueVente(statue)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }

    private fun initStatueVente(statue: Boolean) {
        if (statue) {
            statueTextView.visibility = View.GONE
            venduButton.isEnabled = true
        } else {
            statueTextView.visibility = View.VISIBLE
            venduButton.visibility = View.GONE
            venduButton.isEnabled = false
        }
    }


}