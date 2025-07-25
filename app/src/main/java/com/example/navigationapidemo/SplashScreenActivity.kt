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

import android.Manifest.permission
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.libraries.places.api.Places
import java.util.concurrent.TimeUnit

/** The main activity showing a splash screen and requesting for location permission. */
class SplashScreenActivity : AppCompatActivity() {
  @RequiresApi(VERSION_CODES.P)
  override fun onCreate(bundle: Bundle?) {
    super.onCreate(bundle)
    Places.initialize(applicationContext, getApiKeyFromMetaData())
    setContentView(R.layout.activity_splash_screen)
    val imageView = findViewById<ImageView>(R.id.splash_image)

    Glide.with(this)
      .load("http://services.google.com/fh/files/misc/google_maps_logo_480.png")
      .placeholder(R.drawable.google_maps_logo)
      .fitCenter()
      .into(imageView)

    val permissions =
      if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {

        arrayOf(permission.ACCESS_FINE_LOCATION, permission.POST_NOTIFICATIONS,permission.RECORD_AUDIO,
          permission.FOREGROUND_SERVICE,permission.GET_TASKS,permission.CAMERA)
      } else {
        arrayOf(permission.ACCESS_FINE_LOCATION,permission.RECORD_AUDIO,
          permission.FOREGROUND_SERVICE,permission.GET_TASKS)

      }

//    if (permissions.any { !checkPermissionGranted(it) }) {
//      if (permissions.any { shouldShowRequestPermissionRationaleFix(it) }) {
//        // Display a dialogue explaining the required permissions.
//        // showPermissionRationale(permissions)
//      }
//
//      val permissionsLauncher =
//        registerForActivityResult(
//          RequestMultiplePermissions(),
//          { permissionResults ->
//            if (permissionResults.getOrDefault(permission.ACCESS_FINE_LOCATION, false)) {
//              onLocationPermissionGranted()
//            } else {
//              finish()
//            }
//          },
//        )
//
//      permissionsLauncher.launch(permissions)
//    } else {
//      Handler().postDelayed({ onLocationPermissionGranted() }, SPLASH_SCREEN_DELAY_MILLIS)
//    }
    if (permissions.any { !checkPermissionGranted(it) }) {
      if (permissions.any { shouldShowRequestPermissionRationaleFix(it) }) {
        // You might want to show a dialog explaining why all permissions are needed
        showPermissionRationale(permissions)
      } else {
        requestPermissions(permissions)
      }
    } else {
      Handler().postDelayed({ onAllPermissionsGranted() }, SPLASH_SCREEN_DELAY_MILLIS)
    }
  }
  private fun showPermissionRationale(permissions: Array<String>) {
    AlertDialog.Builder(this)
      .setTitle("Permissions Required")
      .setMessage("This app needs location permission for navigation and audio permission for voice commands. These permissions are essential for the app to function properly.")
      .setPositiveButton("Grant") { _, _ ->
        requestPermissions(permissions)
      }
      .setNegativeButton("Exit") { _, _ ->
        finish()
      }
      .setCancelable(false)
      .show()
  }
  private fun requestPermissions(permissions: Array<String>) {
    val permissionsLauncher = registerForActivityResult(
      RequestMultiplePermissions()
    ) { permissionResults ->
      val allGranted = permissionResults.all { it.value }
      if (allGranted) {
        onAllPermissionsGranted()
      } else {
        // Check specifically which critical permissions were denied
        val locationGranted = permissionResults.getOrDefault(permission.ACCESS_FINE_LOCATION, false)
        val audioGranted = permissionResults.getOrDefault(permission.RECORD_AUDIO, false)

        if (!locationGranted || !audioGranted) {
          // Show dialog explaining that these permissions are required
          AlertDialog.Builder(this)
            .setMessage("Location and audio permissions are required for this app to function.")
            .setPositiveButton("Exit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
        }
      }
    }
    permissionsLauncher.launch(permissions)
  }
  private fun onAllPermissionsGranted() {
    val sharedPref = this.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
   val res: String = sharedPref.getString("saved_contacts","false").toString()
    Log.d("BRUH",res)
    if(res != "false"){
      val mainActivity = Intent(this, NavViewActivity::class.java)
      mainActivity.action = MAIN_ACTIVITY_INTENT_ACTION
      startActivity(mainActivity)
      finish()
    }else{
      val emergency = Intent(this,EmergencyContactsActivity::class.java)
      startActivity(emergency)
      finish()
    }
  }

  private fun checkPermissionGranted(permissionToCheck: String): Boolean =
    ContextCompat.checkSelfPermission(this, permissionToCheck) == PackageManager.PERMISSION_GRANTED

  /**
   * Fixes a known memory leak that occurs in Android 12 in [shouldShowRequestPermissionRationale].
   *
   * Alternatively, you may update androidx.core to 1.10.0+ and use
   * [ActivityCompat.shouldShowRequestPermissionRationale] directly to avoid this workaround.
   * However, there are still edge cases that will fail and still default to the method that leaks.
   * Consider updating [handleShouldShowRequestPermissionRationaleFixFailure] to control what
   * happens in this scenario.
   */
  private fun shouldShowRequestPermissionRationaleFix(permission: String): Boolean =
    // This is very close to the fix you would get from upgrading to androidx.core 1.10.0 (see
    // https://github.com/androidx/androidx/pull/435). However, there are still some edge case
    // where the 1.10.0 fix will fall through and still produce the memory leak. Implement
    // #handleWorkPermissionsAroundFailure to control what happens when it would normally fall
    // through.
    if (VERSION.SDK_INT == VERSION_CODES.S) {
      PackageManager::class
        .java
        .getMethod("shouldShowRequestPermissionRationale", String::class.java)
        .invoke(getApplication().getPackageManager(), permission) as Boolean
        ?: handleShouldShowRequestPermissionRationaleFixFailure(permission)
    } else {
      // This would leak SplashScreenActivity if called in Android 12 (VERSION_CODES.S).
      shouldShowRequestPermissionRationale(permission)
    }

  /** Update this method to control the outcome when the workaround is unsuccessful. */
  private fun handleShouldShowRequestPermissionRationaleFixFailure(permission: String) = true

  private fun onLocationPermissionGranted() {
    val mainActivity = Intent(this, CameraActivity::class.java)
    mainActivity.action = MAIN_ACTIVITY_INTENT_ACTION
    startActivity(mainActivity)
    finish()
  }

  /**
   * Gets the Google Maps Api Key for the Places API from Metadata.
   *
   * @return The API key from AndroidManifest.
   * @throws RuntimeException if meta data com.google.android.geo.API_KEY doesn't exist.
   */
  private fun getApiKeyFromMetaData(): String {
    return try {
      val packageInfo: PackageItemInfo =
        getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA)
      packageInfo.metaData.getString("com.google.android.geo.API_KEY")!!
    } catch (e: PackageManager.NameNotFoundException) {
      throw RuntimeException("com.google.android.geo.API_KEY not defined in Manifest")
    }
  }

  companion object {
    const val MAIN_ACTIVITY_INTENT_ACTION = "com.example.navigationapidemo.intent.action.MAIN"
    private const val MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 100
    private val SPLASH_SCREEN_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(2)
  }
}
