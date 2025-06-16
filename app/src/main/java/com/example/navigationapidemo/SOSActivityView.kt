package com.example.navigationapidemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.VolleyLog.TAG
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import java.util.Vector
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class SOSActivityView : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sos)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            // If permissions are granted, get the current location
            getCurrentLocation()
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnCompleteListener { task: Task<android.location.Location> ->
            if (task.isSuccessful && task.result != null) {
                val location = task.result
                val latitude = location.latitude
                val longitude = location.longitude

                val contacts = Vector<String>()
                val sharedPreferences = this.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
                for (contact in sharedPreferences.all) {
                    if(contact.value.toString() != "true"){
                        contacts.add(contact.value.toString())
                    }
                }
                val progressBar = findViewById<ProgressBar>(R.id.Sos_Progress)
                progressBar.visibility = View.VISIBLE

                val mailRequest = MailHttpRequest(this)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = mailRequest.sendMailRequest(latitude, longitude, contacts)
                        Toast.makeText(this@SOSActivityView, result, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@SOSActivityView, e.message, Toast.LENGTH_LONG).show()
                    }finally {
                        progressBar.visibility = View.GONE
                    }
                }
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, get the location
                getCurrentLocation()
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }
}