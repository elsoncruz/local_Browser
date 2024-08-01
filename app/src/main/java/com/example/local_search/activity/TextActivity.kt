package com.example.local_search.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.local_search.R
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.google.firebase.FirebaseApp

class TextActivity : AppCompatActivity() {

    private lateinit var mCameraSource: CameraSource
    private lateinit var textRecognizer: TextRecognizer
    private val tag: String ="MainActivity"
    private val myPermissionRequestCamera:Int=101
    private var isCameraRunning = false
    private var shouldUpdateTextView = true
    private lateinit var textView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)


        val captureButton = findViewById<Button>(R.id.captureButton)
        captureButton.setOnClickListener {
            if (isCameraRunning) {
                stopCamera()
            } else {
                startCamera()
            }
        }

        FirebaseApp.initializeApp(this)

        requestForPermission()


        textRecognizer = TextRecognizer.Builder(this)
            .build()
        if (!textRecognizer.isOperational) {
            Toast.makeText(this, "Dependencies are not loaded yet...please try after few moment!!", Toast.LENGTH_SHORT)
                .show()
            Log.e(tag, "Dependencies are downloading....try after few moment")
            return
        }
        //  Init camera source to use high resolution and auto focus
        mCameraSource = CameraSource.Builder(applicationContext, textRecognizer)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setAutoFocusEnabled(true)
            .setRequestedFps(30.0f)
            .build()


        val mySurfaceView = findViewById<SurfaceView>(R.id.mySurfaceView1)
        mySurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mCameraSource.stop()
            }

            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (isCameraPermissionGranted()) {
                        mCameraSource.start(mySurfaceView.holder)
                    } else {
                        requestForPermission()
                    }
                } catch (e: Exception) {
                    toast("Error :" + e.message)
                }
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
            }
        })
        textView = findViewById(R.id.textView)
        textRecognizer.setProcessor(object : Detector.Processor<TextBlock> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<TextBlock>) {
                val items = detections.detectedItems

                if (items.size() > 0 && shouldUpdateTextView) {
                    textView.post {
                        val stringBuilder = StringBuilder()
                        for (i in 0 until items.size()) {
                            val item = items.valueAt(i)
                            stringBuilder.append(item.value)
                            stringBuilder.append("\n")
                        }
                        val processedText = postProcessText(stringBuilder.toString())
                        textView.text = processedText
                    }
                    shouldUpdateTextView = false
                }
            }
        })

    }

    private fun startCamera() {
        shouldUpdateTextView = true
    }

    private fun stopCamera() {
        mCameraSource.stop()
        shouldUpdateTextView = false
    }


    private fun postProcessText(text: String): String {

        val processedTextWithUnderscores = text.replace(" ", " ")

        val lines = processedTextWithUnderscores.split("\n")

        val processedLines = lines.map { line ->
            // Process each line here if needed
            line
        }
        val processedText = processedLines.joinToString("\n")


        return processedText
    }





    @Suppress("UNREACHABLE_CODE")
    private fun isCameraPermissionGranted(): Boolean {

        return ContextCompat.checkSelfPermission(
            this@TextActivity, Manifest.permission.CAMERA
        )== PackageManager.PERMISSION_GRANTED

        return false
    }

    private fun requestForPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(
                this@TextActivity,
                Manifest.permission.CAMERA
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@TextActivity,
                    Manifest.permission.CAMERA
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this@TextActivity,
                    arrayOf(
                        Manifest.permission.CAMERA
                    ),
                    myPermissionRequestCamera
                )

                // MY_PERMISSIONS_REQUEST_CAMERA is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }
    }
    fun toast(text : String){
        Toast.makeText(this@TextActivity,text, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            myPermissionRequestCamera -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    requestForPermission()
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}