package com.example.scanglucideaide

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.os.Environment
import com.example.scanglucideaide.MainActivity.Companion.CAMERA_PERMISSION_REQUEST_CODE
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException

class RecBasPro : AppCompatActivity() {
    private lateinit var textView: TextView
    private lateinit var editName: EditText
    private lateinit var editPrice: EditText
    private lateinit var editLoc: EditText
    private lateinit var importDoc: Button
    private lateinit var exportDoc: Button
    private lateinit var returnBtn: Button
    private lateinit var pickPhotoLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private var selectedImageUri: Uri? = null
    private var URL = ""
    private val db = Firebase.firestore

    // Uri pour stocker l'image prise par la caméra
    private lateinit var photoUri: Uri

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.rec_bas_pro)

        // Initialisation des vues
        textView = findViewById(R.id.textView)
        editName = findViewById(R.id.editTextInput)
        editPrice = findViewById(R.id.editTextPrice)
        editLoc = findViewById(R.id.editTextLoc)
        importDoc = findViewById(R.id.buttonImport)
        exportDoc = findViewById(R.id.buttonSave)
        returnBtn = findViewById(R.id.buttonReturn)
        val qrcode = MainActivity.DISPLAYVALUE
        textView.text = qrcode

        // Initialisation des lanceurs d'intents pour galerie et caméra
        initPhotoLaunchers()

        // Gestion de l'import de photo
        importDoc.setOnClickListener {
            showPhotoChoiceDialog()  // Afficher le choix entre galerie et caméra
        }

        // Sauvegarder les données
        exportDoc.setOnClickListener {
            initSaveData()
        }

        initReturn()
    }

    private fun initReturn() {
        returnBtn.setOnClickListener {
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun initSaveData() {
        val name = editName.text.toString()
        val price = editPrice.text.toString()
        val loc = editLoc.text.toString()
        val code = MainActivity.DISPLAYVALUE
        val photo = URL
        val dispo = true

        if (name.isEmpty() || price.isEmpty() || loc.isEmpty() || photo.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and upload a photo", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val user = hashMapOf(
            "name" to name,
            "price" to "$price €",
            "loc" to loc,
            "code" to code,
            "photo" to photo,
            "statue" to dispo
        )

        db.collection("products").document(code).set(user)
            .addOnSuccessListener {
                finish()
                startActivity(Intent(this, MainActivity::class.java))
                MainActivity.DISPLAYVALUE = ""
                Toast.makeText(this, "successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("Firebase", "Error adding document", e)
                Toast.makeText(this, "Product add failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageToFirebase(uri: Uri) {
        val storage = FirebaseStorage.getInstance()
        val storageRef: StorageReference = storage.reference
        val photoRef: StorageReference = storageRef.child("photos/${uri.lastPathSegment}")

        val uploadTask = photoRef.putFile(uri)
        uploadTask.addOnSuccessListener {
            photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                URL = downloadUri.toString()
                Log.d("Firebase", "Photo URL: $URL")
                Toast.makeText(this, "Photo uploaded successfully", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Photo upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initPhotoLaunchers() {
        // Lanceur pour la galerie
        pickPhotoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri: Uri? = result.data?.data
                    selectedImageUri = uri
                    uri?.let {
                        saveImageToFirebase(it)
                    }
                }
            }

        // Lanceur pour la caméra
        takePhotoLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    saveImageToFirebase(photoUri)
                } else {
                    Toast.makeText(this, "Camera capture failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showPhotoChoiceDialog() {
        val options = arrayOf("Choisir une photo", "Prendre une photo")
        AlertDialog.Builder(this)
            .setTitle("Sélectionnez une option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickPhotoFromGallery()
                    1 -> takePhotoWithCamera()
                }
            }
            .show()
    }

    private fun pickPhotoFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        pickPhotoLauncher.launch(intent)
    }

    private fun takePhotoWithCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            return
        }

        try {
            photoUri = createImageFile()
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
            takePhotoLauncher.launch(photoUri)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to create file for photo", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "An unexpected error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): Uri {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("JPEG_${System.currentTimeMillis()}_", ".jpg", storageDir)
        return FileProvider.getUriForFile(this, "com.example.scanglucideaide.fileprovider", file)
    }


}

