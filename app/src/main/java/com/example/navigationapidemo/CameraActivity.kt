package com.example.navigationapidemo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale


class CameraActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private var mCamera: Camera? = null
    private lateinit var sv: SurfaceView
    private lateinit var sHolder: SurfaceHolder
    private lateinit var iv_image: ImageView
    private var parameters: Camera.Parameters? = null
    private var bmp: Bitmap? = null
    private lateinit var textToSpeech: TextToSpeech


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_view)
        initializeTexttoVoice()
        initializeCamera()

    }

    private fun initializeTexttoVoice(){
        textToSpeech = TextToSpeech(
            applicationContext
        ) { i ->
            if (i != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.ENGLISH)
            }
        }
    }

    private fun initializeCamera() {
        val frontCameraIndex = getFrontCameraId()
        if (frontCameraIndex == -1) {
            Toast.makeText(applicationContext, "No front camera available", Toast.LENGTH_LONG).show()
            finish() // Close activity if no front camera
            return
        }

        // Initialize views
        iv_image = findViewById(R.id.imageView)
        sv = findViewById(R.id.surfaceView)
        sHolder = sv.holder
        sHolder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            val frontCameraIndex = getFrontCameraId()
            if (frontCameraIndex != -1) {
                mCamera = Camera.open(frontCameraIndex)
                Toast.makeText(applicationContext, "Front camera initialized", Toast.LENGTH_SHORT).show()

                try {
                    mCamera?.setPreviewDisplay(holder)
                } catch (e: IOException) {
                    releaseCamera()
                    Toast.makeText(applicationContext, "Error setting camera preview", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: RuntimeException) {
            Toast.makeText(applicationContext, "Error opening camera: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (mCamera == null) return

        try {
            parameters = mCamera?.parameters
            mCamera?.parameters = parameters
            mCamera?.startPreview()

            setupPictureCallback()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Error updating camera preview: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupPictureCallback() {
        val pictureCallback = Camera.PictureCallback { data, camera ->
            // Display the captured image
            val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
            iv_image.setImageBitmap(bmp)

            lifecycleScope.launch {
                Log.d("ncnn","BRUH");
                generateDescription(bmp)
            }
        }

        mCamera?.takePicture(null, null, pictureCallback)
    }

    private suspend fun generateDescription(bmp: Bitmap) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.API_KEY
        )

        val button:ProgressBar = findViewById<ProgressBar>(R.id.gemini_call);
        button.visibility=View.VISIBLE;

        val inputContent = content() {
            image(bmp)
            text("Describe the scenery")
        }

        val response = generativeModel.generateContent(inputContent)
        button.visibility=View.GONE;
        response.text?.let { textToSpeech.speak(it,TextToSpeech.QUEUE_FLUSH,null) }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseCamera()
    }

    private fun releaseCamera() {
        mCamera?.also {
            it.stopPreview()
            it.release()
            mCamera = null
        }
    }

    private fun getFrontCameraId(): Int {
        val ci = CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, ci)
            if (ci.facing == CameraInfo.CAMERA_FACING_FRONT) {
                return i
            }
        }
        return -1 // No front-facing camera found
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
    }
}