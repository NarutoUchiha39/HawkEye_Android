/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.navigationapidemo

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.util.Arrays

/** An activity to host AutocompleteSupportFragment from Places SDK. */
class PlacePickerActivity : AppCompatActivity() {
  private lateinit var broadcastReceiver: BroadcastReceiver
  private val placesClient: PlacesClient by lazy { Places.createClient(this) }

  @SuppressLint("NewApi")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_place_picker)

    // Handle destination query from VoiceCommandService
//    if (intent.action == "com.example.app.ACTION_SET_DESTINATION") {
//      val destinationQuery = intent.getStringExtra("DESTINATION_QUERY")
//      destinationQuery?.let { query ->
//        findPlaceAndReturnResult(query)
//      }
//    }
    broadcastReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.NAVIGATE_TO_DESTINATION") {
          val destinationQuery = intent.getStringExtra("DESTINATION_QUERY")
          destinationQuery?.let { query ->
            findPlaceAndReturnResult(query)
          }
        }
      }
    }
    // Register the broadcast receiver
    registerReceiver(broadcastReceiver, IntentFilter("android.intent.action.NAVIGATE_TO_DESTINATION"),
        RECEIVER_EXPORTED
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(broadcastReceiver)
  }
  private fun findPlaceAndReturnResult(query: String) {
    val request = FindAutocompletePredictionsRequest.newInstance(query)
    placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
      val predictions = response.autocompletePredictions
      if (predictions.isNotEmpty()) {
        val firstPrediction = predictions[0]
        val placeId = firstPrediction.placeId

        // Fetch place details using placeId
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val fetchPlaceRequest = FetchPlaceRequest.newInstance(placeId, placeFields)
        placesClient.fetchPlace(fetchPlaceRequest)
          .addOnSuccessListener { fetchPlaceResponse ->
            val place = fetchPlaceResponse.place
            setResult(RESULT_OK, Intent().putExtra("PLACE", place))
            finish()
          }
          .addOnFailureListener { exception ->
            handleError(exception)
          }
      } else {
        handleError(Exception("No places found for query: $query"))
      }
    }
      .addOnFailureListener { exception ->
        handleError(exception)
      }
  }

  private fun handleError(exception: Exception) {
    setResult(RESULT_CANCELED, Intent(exception.message))
  }

  companion object {
    fun getPlace(data: Intent): Place {
      return data.getParcelableExtra("PLACE")!!
    }
  }
}