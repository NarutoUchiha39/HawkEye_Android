package com.example.navigationapidemo

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer

class VoiceCommandService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioRecord: AudioRecord? = null
    private var recognizer: Recognizer? = null
    private var isListening = false
    private lateinit var model: Model

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceCommandChannel"
        private const val CHANNEL_NAME = "Voice Command Service"

        const val ACTION_START_LISTENING = "ACTION_START_LISTENING"
        const val ACTION_STOP_LISTENING = "ACTION_STOP_LISTENING"
        const val ACTION_COMMAND_DETECTED = "ACTION_COMMAND_DETECTED"
    }

    override fun onCreate() {
        super.onCreate()
        model = VoskModelManager.getInstance(this).model
        createNotificationChannel()
        val notification = createNotification()
//        initializeVosk()
//        startForeground(NOTIFICATION_ID,notification)
        initializeVosk()
        startListening()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
//                .apply {
//                description = "Voice command detection service"
//                enableLights(true)
//                lightColor = Color.BLUE
//                setSound(null, null) // No sound for service notifications
//            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
//            val notificationManager = getSystemService(NotificationManager::class.java)
//            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Command Service")
            .setContentText("Listening for commands...")
            .setSmallIcon(R.drawable.notification_icon) // Replace with your icon
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createForegroundNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Add stop action to notification
        val stopIntent = Intent(this, VoiceCommandService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Command Service")
            .setContentText("Listening for voice commands...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Listening",
                stopPendingIntent
            )
            .build()
    }

    private fun initializeVosk() {
        serviceScope.launch {
            try {
//                model = Model(assets, "model")
                recognizer = Recognizer(model, 16000.0f).apply {
                    setWords(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> startListening()
            ACTION_STOP_LISTENING -> stopListening()
        }
        return START_STICKY
    }

    private fun startListening() {
        if (isListening) return

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        isListening = true

        serviceScope.launch {
            startVoiceRecognition()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun stopListening() {
        isListening = false
        audioRecord?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private suspend fun startVoiceRecognition() = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )


        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val buffer = ByteArray(4096)
        audioRecord?.startRecording()

        while (isListening && isActive) {

            val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (readSize > 0) {
                recognizer?.let { rec ->
                    if (rec.acceptWaveForm(buffer, readSize)) {
                        val result = rec.result
                        val finalResult = rec.finalResult
                        Log.d("kedar debug : ","voice note"+result)
                        var firstIndex:Int = result.length - 3
                        var lastIndex:Int = result.length - 3
                        while(firstIndex>=0){
                            firstIndex--
                            if(result[firstIndex] == '"'){
                                firstIndex++
                                break
                            }
                        }
                        val answer:String = result.substring(firstIndex,lastIndex)
                        Log.d("result","FULL RESULT : "+result)
                        Log.d("ANSWER","ANSWER : "+answer)
                        processVoiceCommand(answer)
                    }
                }
            }
        }
    }

    private fun checkCurrentActivity(): String {
        val activityManager =
            getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)
        val topActivityComponentName = runningTasks[0].topActivity
        return topActivityComponentName?.className ?: ""
    }
    private fun playAudio(audioFile :Int) {
        // Play audio through speaker
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = true

        val mediaPlayer = MediaPlayer.create(this, audioFile)
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
        mediaPlayer.start()
    }

    private fun processVoiceCommand(result: String) {
        if(result == "start navigation"){
            Log.d("Starting Navigation","STARTING NAVIGATION")
            navigateToActivity(NavViewActivity::class.java)
        }
        else if(result == "help"){
            Log.d("HELP","HELP IS ON THE WAY")
            navigateToActivity(SOSActivityView::class.java)
        } else if(result=="destination"){
            if(checkCurrentActivity() == NavViewActivity::class.java.name){
                Log.d("DESTINATION","SET DESTINATION IS ON THE WAY")
//                navigateToActivity(PlacePickerActivity::class.java)
                val setDestination = Intent("android.intent.action.SET_DESTINATION")
                sendBroadcast(setDestination)
            } else {
                // play audio through speaker "please go to navigation activity"
                Log.d("DESTINATION","Not in nav view activity")
                // write code to play a audio file from speakers
                playAudio(R.raw.go_to_nav_view)
            }
        } else if(checkCurrentActivity() == PlacePickerActivity::class.java.name) {
            Log.d("DESTINATION","destination : "+result)
//                navigateToActivity(PlacePickerActivity::class.java)
            val destination:Intent = Intent("android.intent.action.NAVIGATE_TO_DESTINATION",)
            destination.putExtra("DESTINATION_QUERY",result)
            sendBroadcast(destination)
        } else if(result == "describe scenery"){
            Log.d("Scenery","Launching describe scenery")
            navigateToActivity(CameraActivity::class.java)
        }

    }
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
    private fun broadcastCommand(command: String) {
        Intent(ACTION_COMMAND_DETECTED).apply {
            putExtra("command", command)
            sendBroadcast(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        serviceScope.cancel()
        recognizer?.close()
        model?.close()
    }
}