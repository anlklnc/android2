package com.anilkilinc.superlivetutorial.video

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.anilkilinc.superlivetutorial.R
import com.anilkilinc.superlivetutorial.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {

    private lateinit var binding:ActivityStartBinding
    var isProceedPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val button =  binding.btnStart
        button.setOnClickListener {
            checkVideoPermissions()
        }
    }

    override fun onStart() {
        super.onStart()
        if (isProceedPressed) {
            isProceedPressed = false
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // Permission was already granted. Continue to the video page
                startVideo()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission())
        { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue to the video page
                println("permission just granted")
                startVideo()
            } else {
                //show dialog
                println("permission denied")
                showPermissionDialog()
            }
        }

    private fun checkVideoPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission was already granted. Continue to the video page
            startVideo()
        } else {
            //permissions needed
            requestPermissionLauncher.launch(
                Manifest.permission.CAMERA)
        }
    }

    private fun startVideo() {
        startActivity(Intent(this, VideoActivity::class.java))
    }

    private fun showPermissionDialog() {

        AlertDialog.Builder(this)
            .setMessage(getString(R.string.request_video_permission_message))
            .setPositiveButton(getString(R.string.proceed_button)) { arg0, arg1 ->
                isProceedPressed = true
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel_button)) { arg0, arg1 -> }
            .create().show()
    }
}