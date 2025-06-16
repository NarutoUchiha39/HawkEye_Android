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
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import org.vosk.Model

/** Main activity that lets the user choose a demo to launch. */
class MainActivity : AppCompatActivity() {
//  var vosModel: Model? = null
  @RequiresApi(Build.VERSION_CODES.O)
  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    val listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, DEMOS.keys.toList())
    val listView = findViewById<ListView>(R.id.list_view)
    val SOSButton = findViewById<Button>(R.id.SOSButton)
    val handler = Handler(Looper.getMainLooper())
    var isHolding = false
    listView.adapter = listAdapter
    listView.onItemClickListener = OnItemClickListener { parent, view, position, _ ->
      val demoName = parent.getItemAtPosition(position) as String
      startActivity(Intent(view.context, DEMOS[demoName]))
    }

//    SOSButton.setOnClickListener {
//      startActivity(Intent(this, SOSActivityView::class.java))
//    }
    SOSButton.setOnTouchListener { v, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          isHolding = true
          // Start a new thread to handle the long press
          handler.postDelayed({
            if (isHolding) {
              // Code to execute after holding for 5 seconds
              v.performClick() // Call performClick for accessibility support
              startActivity(Intent(this, SOSActivityView::class.java))
            }
          }, 5000) // 5000 milliseconds = 5 seconds
          true // Return true to indicate the event is consumed
        }

        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
          // Remove the callback if the user releases the button before 5 seconds
          isHolding = false
          handler.removeCallbacksAndMessages(null) // Correctly remove callbacks
          false // Return false to allow other touch events
        }

        else -> false // Handle other touch events

      }
    }
    val serviceIntent = Intent(this, VoiceCommandService::class.java)
    startForegroundService(serviceIntent)
  }

  companion object {
    private val DEMOS =
      mapOf<String, Class<*>>(
        "Start Navigation" to NavViewActivity::class.java,
//        "NavFragmentActivity" to NavFragmentActivity::class.java,
//        "SwappingMapAndNavActivity" to SwappingMapAndNavActivity::class.java,
      )
  }
}