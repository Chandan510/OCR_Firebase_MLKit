package com.example.mldemo.Utils

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore

class Constants {

    fun showImageChooser(activity: Activity){
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activity.startActivityForResult(galleryIntent, 101)
    }

}