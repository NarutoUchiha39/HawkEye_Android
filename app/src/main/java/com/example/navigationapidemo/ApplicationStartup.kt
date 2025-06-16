package com.example.navigationapidemo

import android.app.Application
import android.content.BroadcastReceiver
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import org.vosk.Model

class ApplicationStartup : Application() {
    companion object {

        private lateinit var instance: ApplicationStartup
        private lateinit var requestQueue: RequestQueue

        fun getRequestQueue(): RequestQueue {
            return requestQueue
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        requestQueue = Volley.newRequestQueue(applicationContext)
        VoskModelManager.getInstance(this).initModel(object : VoskModelManager.ModelInitializationCallback {
            override fun onSuccess(model: Model) {
                // Model initialized successfully
                Log.d("VoskModelManager", "Model initialized successfully")
            }

            override fun onFailure(errorMessage: String) {
                // Handle initialization failure
                Log.e("VoskModelManager", "Model initialization failed: $errorMessage")
            }
        })
    }
}