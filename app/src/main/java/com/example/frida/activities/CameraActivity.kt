package com.example.frida.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.frida.Constants
import com.example.frida.R
import com.example.frida.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CameraActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null

    private val outputDirectory: File by lazy {
        getDirectory()
    }

    private val binding: ActivityCameraBinding by lazy {
        ActivityCameraBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.takePhotoButton.setOnClickListener {
            takePhoto()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(Constants.FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    val intent = Intent().putExtra(IMAGE_URI, savedUri.toString())
                    setResult(RESULT_SUCCESSFUL, intent)
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }

            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {

        val preview: Preview = Preview.Builder()
            .build()

        imageCapture = ImageCapture.Builder()
            .build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

        // Getting the CameraInfo instance from the camera
        val cameraInfo = camera.cameraInfo

        // Use the CameraInfo instance to observe the zoom state
        cameraInfo.zoomState.observe(this) {
            val currentZoomRatio = it.zoomRatio
            val currentLinearZoom = it.linearZoom

            Log.d(TAG, "Zoom ratio: $currentZoomRatio")
            Log.d(TAG, "Linear ratio: $currentLinearZoom")

        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getDirectory(): File {
        val mediaDir = externalMediaDirs.lastOrNull()?.let {
            Log.d("CameraActivity", it.absolutePath)
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 7
        const val RESULT_SUCCESSFUL = "result_successful".length
        const val IMAGE_URI = "image_uri"
        fun getIntent(context: Context) =
            Intent(context, CameraActivity::class.java)

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}