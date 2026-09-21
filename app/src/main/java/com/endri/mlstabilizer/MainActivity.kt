package com.endri.mlstabilizer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var latencyText: TextView
    private lateinit var jitterText: TextView
    private lateinit var lossText: TextView

    private val handler = Handler(Looper.getMainLooper())

    private var lastLatency = -1L
    private var totalTests = 0
    private var failedTests = 0

    private val pingTask = object : Runnable {
        override fun run() {
            Thread {
                val start = System.nanoTime()

                try {
                    Socket().use { socket ->
                        socket.connect(
                            InetSocketAddress("1.1.1.1", 443),
                            2000
                        )
                    }

                    val latency =
                        (System.nanoTime() - start) / 1_000_000

                    val jitter =
                        if (lastLatency >= 0) {
                            abs(latency - lastLatency)
                        } else {
                            0
                        }

                    lastLatency = latency
                    totalTests++

                    runOnUiThread {
                        latencyText.text = "Ping: ${latency} ms"
                        jitterText.text = "Jitter: ${jitter} ms"
                        lossText.text =
                            "Packet Loss: ${calculateLoss()}%"
                        status.text = "Status: CONNECTED"
                    }

                } catch (_: Exception) {

                    totalTests++
                    failedTests++

                    runOnUiThread {
                        latencyText.text = "Ping: -- ms"
                        jitterText.text = "Jitter: -- ms"
                        lossText.text =
                            "Packet Loss: ${calculateLoss()}%"
                        status.text = "Status: NETWORK ERROR"
                    }
                }

                handler.postDelayed(this, 3000)
            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
        }

        fun text(value: String, size: Float): TextView {
            return TextView(this).apply {
                text = value
                textSize = size
                setPadding(0, 8, 0, 8)
            }
        }

        root.addView(text("🎮 ML STABILIZER ENDRI", 26f))
        root.addView(text("Game Network Optimizer", 17f))
        root.addView(text("Dibuat oleh Endri", 14f))

        status = text("Status: READY", 18f)
        root.addView(status)

        latencyText = text("Ping: -- ms", 22f)
        root.addView(latencyText)

        jitterText = text("Jitter: -- ms", 18f)
        root.addView(jitterText)

        lossText = text("Packet Loss: 0%", 18f)
        root.addView(lossText)

        val start = Button(this).apply {
            text = "START MONITOR"
        }

        start.setOnClickListener {
            startService(
                Intent(
                    this@MainActivity,
                    NetworkMonitorService::class.java
                )
            )

            status.text = "Status: MONITORING"

            handler.removeCallbacks(pingTask)
            handler.post(pingTask)
        }

        root.addView(start)

        val stop = Button(this).apply {
            text = "STOP MONITOR"
        }

        stop.setOnClickListener {
            stopService(
                Intent(
                    this@MainActivity,
                    NetworkMonitorService::class.java
                )
            )

            handler.removeCallbacks(pingTask)

            status.text = "Status: STOPPED"
        }

        root.addView(stop)

        val network = Button(this).apply {
            text = "NETWORK SETTINGS"
        }

        network.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            )
        }

        root.addView(network)

        root.addView(
            text(
                "Monitoring latency jaringan saat bermain.",
                13f
            )
        )

        root.addView(
            text(
                "Ping diukur melalui koneksi TCP ke Cloudflare 1.1.1.1:443.",
                12f
            )
        )

        setContentView(root)
    }

    private fun calculateLoss(): Int {
        if (totalTests == 0) return 0
        return (failedTests * 100) / totalTests
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
