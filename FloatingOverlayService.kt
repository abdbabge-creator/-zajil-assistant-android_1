// خدمة الخلفية العائمة الدائمة فوق التطبيقات (SYSTEM_ALERT_WINDOW + Foreground Service)

package com.smartassistant.arabic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * FloatingOverlayService: Keeps the animated Assistant character floating
 * continuously over all apps (YouTube, WhatsApp, Messenger, Games, Browser).
 */
class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_character, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // Draggable touch listener across screen
        floatingView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    if (diffX < 10 && diffY < 10) {
                        // User tapped on the assistant character!
                        onAssistantCharacterTapped()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(floatingView, params)
    }

    private fun onAssistantCharacterTapped() {
        // Trigger voice listening or express joy
        VoiceAssistantManager.getInstance(applicationContext).startListening()
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "floating_assistant_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "المساعد الذكي العائم",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("المساعد الذكي نشط")
            .setContentText("الشخصية العائمة تعمل فوق التطبيقات وتستمع لأوامرك الصوتية")
            .setSmallIcon(R.drawable.ic_assistant_bird)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager.removeView(floatingView)
        }
    }
}
