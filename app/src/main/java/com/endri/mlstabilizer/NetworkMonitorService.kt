package com.endri.mlstabilizer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.net.InetAddress

class NetworkMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val task = object : Runnable {
        override fun run() {
            Thread {
                val start = System.nanoTime()

                try {
                    InetAddress.getByName("1.1.1.1")

                    val latency =
                        (System.nanoTime() - start) / 1_000_000

                    updateNotification("Latency: ${latency} ms")

                } catch (_: Exception) {

                    updateNotification("Network check: FAILED")
                }

                handler.postDelayed(this, 3000)
            }.start()
        }
    }

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            "ml_stabilizer",
            "ML Stabilizer Endri",
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        startForeground(
            1001,
            notification("Monitoring jaringan...")
        )

        handler.post(task)
    }

    private fun notification(message: String) =
        NotificationCompat.Builder(this, "ml_stabilizer")
            .setContentTitle("ML Stabilizer Endri")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java)
            .notify(
                1001,
                notification(message)
            )
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
