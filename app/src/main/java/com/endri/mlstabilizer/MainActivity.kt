package com.endri.mlstabilizer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var networkText: TextView
    private lateinit var latencyText: TextView
    private lateinit var jitterText: TextView
    private lateinit var lossText: TextView
    private lateinit var minText: TextView
    private lateinit var avgText: TextView
    private lateinit var maxText: TextView

    private val handler = Handler(Looper.getMainLooper())

    private var lastLatency = -1L
    private var totalTests = 0
    private var failedTests = 0

    private var minLatency = Long.MAX_VALUE
    private var maxLatency = 0L
    private var totalLatency = 0L

    private var mlMode = false

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

                    if (latency < minLatency) {
                        minLatency = latency
                    }

                    if (latency > maxLatency) {
                        maxLatency = latency
                    }

                    totalLatency += latency

                    val average =
                        totalLatency / totalTests

                    runOnUiThread {

                        latencyText.text =
                            "PING       ${latency} ms"

                        jitterText.text =
                            "JITTER     ${jitter} ms"

                        lossText.text =
                            "PACKET LOSS ${calculateLoss()} %"

                        minText.text =
                            "MIN        ${minLatency} ms"

                        avgText.text =
                            "AVG        ${average} ms"

                        maxText.text =
                            "MAX        ${maxLatency} ms"

                        status.text =
                            "STATUS     ${connectionStatus()}"

                        updateStability(latency, jitter)
                    }

                } catch (_: Exception) {

                    totalTests++
                    failedTests++

                    runOnUiThread {

                        latencyText.text =
                            "PING       -- ms"

                        jitterText.text =
                            "JITTER     -- ms"

                        lossText.text =
                            "PACKET LOSS ${calculateLoss()} %"

                        status.text =
                            "STATUS     NETWORK ERROR"
                    }
                }

                val delay =
                    if (mlMode) 1000L else 3000L

                handler.postDelayed(this, delay)

            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createInterface()
    }

    private fun createInterface() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        fun text(value: String, size: Float): TextView {
            return TextView(this).apply {
                text = value
                textSize = size
                setPadding(0, 6, 0, 6)
            }
        }

        root.addView(
            text("🎮 ML STABILIZER ENDRI", 26f)
        )

        root.addView(
            text("Game Network Optimizer", 17f)
        )

        root.addView(
            text("Dibuat oleh Endri", 14f)
        )

        status = text(
            "STATUS     READY",
            18f
        )
        root.addView(status)

        networkText = text(
            "NETWORK    ${connectionStatus()}",
            17f
        )
        root.addView(networkText)

        root.addView(
            text("━━━━━━━━━━━━━━━━━━━━", 14f)
        )

        latencyText = text(
            "PING       -- ms",
            22f
        )
        root.addView(latencyText)

        jitterText = text(
            "JITTER     -- ms",
            18f
        )
        root.addView(jitterText)

        lossText = text(
            "PACKET LOSS 0 %",
            18f
        )
        root.addView(lossText)

        minText = text(
            "MIN        -- ms",
            16f
        )
        root.addView(minText)

        avgText = text(
            "AVG        -- ms",
            16f
        )
        root.addView(avgText)

        maxText = text(
            "MAX        -- ms",
            16f
        )
        root.addView(maxText)

        root.addView(
            text("━━━━━━━━━━━━━━━━━━━━", 14f)
        )

        val mlButton = Button(this).apply {
            text = "🎮 START ML MODE"
        }

        mlButton.setOnClickListener {

            mlMode = true

            resetStatistics()

            startService(
                Intent(
                    this@MainActivity,
                    NetworkMonitorService::class.java
                )
            )

            status.text = "STATUS     ML MODE ON"

            handler.removeCallbacks(pingTask)
            handler.post(pingTask)

            mlButton.text = "🎮 ML MODE ACTIVE"
        }

        root.addView(mlButton)

        val normalButton = Button(this).apply {
            text = "START MONITOR"
        }

        normalButton.setOnClickListener {

            mlMode = false

            resetStatistics()

            startService(
                Intent(
                    this@MainActivity,
                    NetworkMonitorService::class.java
                )
            )

            status.text = "STATUS     MONITORING"

            handler.removeCallbacks(pingTask)
            handler.post(pingTask)

            mlButton.text = "🎮 START ML MODE"
        }

        root.addView(normalButton)

        val stopButton = Button(this).apply {
            text = "STOP MONITOR"
        }

        stopButton.setOnClickListener {

            mlMode = false

            handler.removeCallbacks(pingTask)

            stopService(
                Intent(
                    this@MainActivity,
                    NetworkMonitorService::class.java
                )
            )

            status.text = "STATUS     STOPPED"

            mlButton.text = "🎮 START ML MODE"
        }

        root.addView(stopButton)

        val networkButton = Button(this).apply {
            text = "NETWORK SETTINGS"
        }

        networkButton.setOnClickListener {

            startActivity(
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            )
        }

        root.addView(networkButton)

        root.addView(
            text(
                "ML Mode melakukan pengukuran setiap 1 detik.",
                13f
            )
        )

        root.addView(
            text(
                "Endpoint pengujian: Cloudflare 1.1.1.1:443",
                12f
            )
        )

        setContentView(root)
    }

    private fun resetStatistics() {

        lastLatency = -1L
        totalTests = 0
        failedTests = 0

        minLatency = Long.MAX_VALUE
        maxLatency = 0L
        totalLatency = 0L

        minText.text = "MIN        -- ms"
        avgText.text = "AVG        -- ms"
        maxText.text = "MAX        -- ms"
    }

    private fun calculateLoss(): Int {

        if (totalTests == 0) {
            return 0
        }

        return (failedTests * 100) / totalTests
    }

    private fun connectionStatus(): String {

        val cm =
            getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        val network =
            cm.activeNetwork ?: return "OFFLINE"

        val capabilities =
            cm.getNetworkCapabilities(network)
                ?: return "OFFLINE"

        return when {

            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) -> "Wi-Fi"

            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) -> {

                val tm =
                    getSystemService(
                        Context.TELEPHONY_SERVICE
                    ) as TelephonyManager

                tm.networkOperatorName.ifBlank {
                    "MOBILE DATA"
                }
            }

            else -> "CONNECTED"
        }
    }

    private fun updateStability(
        latency: Long,
        jitter: Long
    ) {

        val stability = when {

            latency > 150 || jitter > 50 ->
                "STATUS     🔴 UNSTABLE"

            latency > 80 || jitter > 25 ->
                "STATUS     🟡 MODERATE"

            else ->
                "STATUS     🟢 STABLE"
        }

        status.text = stability
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }
}
