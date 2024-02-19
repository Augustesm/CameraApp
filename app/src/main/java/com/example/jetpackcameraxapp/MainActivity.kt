package com.example.jetpackcameraxapp

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import com.example.jetpackcameraxapp.ui.theme.JetpackCameraXAppTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission() ) {}
    private lateinit var cameraExecutor: Executor
    private lateinit var speechRecognizer: SpeechRecognizer // balso iskvietimas

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == "com.example.jetpackcameraxapp.OPEN_CAMERA") {
            // Handle the action to open the camera
            // You can add your existing camera opening logic here
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        cameraExecutor = Executors.newSingleThreadExecutor()
        checkVoicePermission()
        setContent {
            JetpackCameraXAppTheme {
                val imageCapture = remember{
                    ImageCapture.Builder().build()
                }
                Box(modifier = Modifier.fillMaxSize()){
                    CameraPreview(modifier = Modifier.fillMaxSize(), imageCapture = imageCapture)
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                    IconButton(onClick = { captureImage(imageCapture, this@MainActivity) }) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Take a photo", modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                        )
                    }
                    }
                }
            }
        }
    }

    private fun checkVoicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionChecker.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PermissionChecker.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                speechRecognizer = initializeSpeechRecognizer()
            }
        }
    }

    private fun captureImage(imageCapture: ImageCapture, context: Context) {
        val file = File(context.cacheDir, "img.jpg")

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                println("the Uri is ${outputFileResults.savedUri}")

                MainScope().launch {
                    try {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                        // Add text to the bitmap
                        val bitmapWithText = addTextToBitmap(bitmap, "Auguste Smolskaite")

                        // Save the modified bitmap to a new file
                        saveBitmapToFile(bitmapWithText, file)

                        // Save to gallery and send email
                        saveToGallery(file, context)
                        mailWithText(context, file)

                    } catch (e: Exception) {
                        println("Error $e")
                        Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                println("Error ${exception.message}")
            }
        })
    }

    private fun addTextToBitmap(bitmap: Bitmap, text: String): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val paint = Paint().apply {
            color = Color.RED
            textSize = 100f
            isAntiAlias = true
        }

        val x = 50f
        val y = canvas.height - 50f

        canvas.drawText(text, x, y, paint)
        return mutableBitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        try {
            val fOut: OutputStream = FileOutputStream(file)

            // Compress the Bitmap and save it to the file
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut)

            fOut.flush()
            fOut.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}

    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
        cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
        imageCapture: ImageCapture
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val coroutineScope = rememberCoroutineScope()
        AndroidView(factory = { context ->
            val previewView = PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                this.scaleType = scaleType
            }

            val previewUseCase = Preview.Builder().build()
            previewUseCase.setSurfaceProvider(previewView.surfaceProvider)

            coroutineScope.launch {
                val cameraProvider = context.cameraProvider()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase, imageCapture)
            }

            previewView
        })
    }
    private fun mailWithText(context: Context, file: File){
        val email = Intent(Intent.ACTION_SEND)
        email.type = "image/*"
        email.putExtra(Intent.EXTRA_SUBJECT, "Android lB3")
        email.putExtra(Intent.EXTRA_EMAIL, arrayOf("auguste.smolskaite@gmail.com"))
        email.putExtra(Intent.EXTRA_TEXT, "Android Studio camera app Auguste Smolskaite")

        val fileProviderUri = FileProvider.getUriForFile(
            context,
            "com.example.jetpackcameraxapp.provider",
            file
        )

        email.putExtra(Intent.EXTRA_STREAM, fileProviderUri)
        email.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        //context.startActivity(Intent.createChooser(email, "select email app"))
        email.setPackage("com.microsoft.office.outlook")

        try {
            context.startActivity(Intent.createChooser(email, "send email"))
        } catch (e: Exception){
            println("Error: $e")
            Toast.makeText(context, "Failed to send email", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveToGallery(file: File, context: Context) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            contentResolver.openOutputStream(it).use { outputStream ->
                outputStream?.write(file.readBytes())
                // Write text to the image (metadata or overlay)
            }
            Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "failed to save image", Toast.LENGTH_SHORT).show()
        }
    }





suspend fun Context.cameraProvider() : ProcessCameraProvider = suspendCoroutine{ continuation ->
        val listenableFuture = ProcessCameraProvider.getInstance(this)
        listenableFuture.addListener({
            continuation.resume(listenableFuture.get())
        }, ContextCompat.getMainExecutor(this))

}