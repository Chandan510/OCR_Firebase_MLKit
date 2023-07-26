package com.example.mldemo.UI


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.mldemo.R
import com.example.mldemo.databinding.ActivityMainBinding
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private var selected_image_uri: Uri? = null
    private val REQUEST_CODE_EXTERNAL_STORAGE = 100
    private val REQUEST_CODE_CAMERA = 102
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        progressBar = binding.loadingProgressBar
        progressBar!!.visibility = View.INVISIBLE


        pickMedia = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uris ->
            Log.e("@@@", uris.toString())
            selected_image_uri = uris
            GlideLoadImage()
            if (selected_image_uri != null) {
                textRecognition()
            }
        }

        binding.btnCam.setOnClickListener(this)
        binding.btnGallery.setOnClickListener(this)


    }

    private fun textRecognition() {
        progressBar!!.visibility = View.VISIBLE
        val firebaseVisionImg = FirebaseVisionImage.fromFilePath(this, selected_image_uri!!)
        val firebaseInstance = FirebaseVision.getInstance()
        val firebaseVisionTextRecog = firebaseInstance.onDeviceTextRecognizer


        val task = firebaseVisionTextRecog.processImage(firebaseVisionImg)
        task.addOnSuccessListener { resultText: FirebaseVisionText ->
            val text = resultText.text
            binding.resultedText.setText(text)
            progressBar!!.visibility = View.INVISIBLE
        }
            .addOnFailureListener { e: Exception ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                progressBar!!.visibility = View.INVISIBLE
            }
    }

    override fun onClick(p0: View?) {
        when (p0!!.id) {

            R.id.btn_gallery -> {

                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

            }

            R.id.btn_cam -> {
                if ((ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.CAMERA
                    )) == PackageManager.PERMISSION_GRANTED
                ) {
                    val camera_intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(camera_intent, REQUEST_CODE_CAMERA)
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.CAMERA),
                        REQUEST_CODE_CAMERA
                    )
                }

            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val camera_intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(camera_intent, REQUEST_CODE_CAMERA)
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAMERA) {
                if (data != null) {
                    try {

                        val extractData = data?.extras?.get("data") as Bitmap
                        Log.e("@@", extractData.toString())



                        selected_image_uri = saveImageToStorage(extractData)
                        Log.e("@@", selected_image_uri.toString())
                        GlideLoadImage()
                        if (selected_image_uri != null) {
                            textRecognition()
                        }
                    } catch (e: IOException) {
                        Toast.makeText(
                            this,
                            "Unable to take picture from camera",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }


    private fun GlideLoadImage() {
        if (selected_image_uri != null) {
            Glide.with(this).load(selected_image_uri).into(binding.selectedImg)
        }
    }

    private fun saveImageToStorage(bitmap: Bitmap): Uri {

        val fileName = "pic_${System.currentTimeMillis()}.jpg"

        val directory = ContextWrapper(applicationContext).getDir("images", Context.MODE_PRIVATE)
        val imageFile = File(directory, fileName)

        try {
            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
        } catch (e: IOException) {
            Log.e("catch", "File not saved")
        }

        return Uri.fromFile(imageFile)

    }

}