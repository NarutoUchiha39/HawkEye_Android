package com.example.navigationapidemo

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.util.Vector
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MailHttpRequest(private val context: Context) {
    private val url = "https://midsemevalapp.onrender.com/mail"

    suspend fun sendMailRequest(
        lat: Double,
        long: Double,
        contacts: Vector<String>
    ): String = suspendCancellableCoroutine { continuation ->
        val jsonBody = JSONObject().apply {
            put("lat", lat)
            put("long", long)
            put("contacts", JSONArray(contacts))
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                continuation.resume("Mail sent successfully!")
            },
            { error ->
                continuation.resumeWithException(error)
            }
        )

        ApplicationStartup.getRequestQueue().add(request)

        continuation.invokeOnCancellation {
            request.cancel()
        }
    }
}