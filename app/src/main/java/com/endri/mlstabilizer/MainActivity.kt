package com.endri.mlstabilizer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var ping: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32,28,32,28)
        }
        fun tv(s:String, z:Float) = TextView(this).apply { text=s; textSize=z; setPadding(0,8,0,8) }

        root.addView(tv("🎮  ML STABILIZER ENDRI",26f))
        root.addView(tv("Game Network Optimizer",16f))
        root.addView(tv("Dibuat oleh Endri",14f))
        status=tv("Status: READY",18f); root.addView(status)
        ping=tv("Latency: -- ms",18f); root.addView(ping)

        val start=Button(this).apply{text="START MONITOR"}
        root.addView(start)
        start.setOnClickListener {
            startService(Intent(this, NetworkMonitorService::class.java))
            status.text="Status: MONITORING"
            Toast.makeText(this,"Monitoring jaringan aktif",Toast.LENGTH_SHORT).show()
        }
        val stop=Button(this).apply{text="STOP MONITOR"}
        root.addView(stop)
        stop.setOnClickListener { stopService(Intent(this,NetworkMonitorService::class.java)); status.text="Status: STOPPED" }

        val net=Button(this).apply{text="NETWORK SETTINGS"}
        root.addView(net)
        net.setOnClickListener { startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }

        root.addView(tv("Aplikasi membantu memantau latency jaringan saat bermain. Android tidak mengizinkan aplikasi biasa memaksa routing operator atau LTE tanpa akses khusus.",13f))
        setContentView(root)
    }
}