package com.example.navigationapidemo

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyLog.TAG
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class MySingleton private constructor(context: Context) {
    private val appContext: Context = context.applicationContext

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(appContext)
    }

    companion object {
        @Volatile
        private var instance: MySingleton? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: MySingleton(context).also { instance = it }
            }
    }

    fun sendMailRequest(lat: Double, long: Double) {
        val url = "http://192.168.0.108:5000/mail"

        val jsonBody = JSONObject().apply {
            put("lat", lat)
            put("long", long)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                try {
                    Toast.makeText(appContext, "Success: ${response.toString()}", Toast.LENGTH_LONG).show()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->
                Log.e(TAG, "Error in sendMailRequest", error)
            }
        )

        requestQueue.add(request)
    }
}