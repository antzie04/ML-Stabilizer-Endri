package com.endri.mlstabilizer

import android.app.*
import android.content.Intent
import android.os.*
import java.net.InetAddress

class NetworkMonitorService : Service() {
    private val handler=Handler(Looper.getMainLooper())
    private val task=object:Runnable{
        override fun run(){
            Thread{
                val t=System.nanoTime()
                try{
                    InetAddress.getByName("1.1.1.1")
                    val ms=(System.nanoTime()-t)/1_000_000
                    notify("Latency check: ${ms} ms")
                }catch(_:Exception){ notify("Network check: failed") }
                handler.postDelayed(this,3000)
            }.start()
        }
    }
    override fun onCreate(){
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("ml","ML Stabilizer Endri",NotificationManager.IMPORTANCE_LOW))
        startForeground(1001, n("Monitoring jaringan"))
        handler.post(task)
    }
    private fun n(s:String)=Notification.Builder(this,"ml")
        .setContentTitle("ML Stabilizer Endri").setContentText(s)
        .setSmallIcon(android.R.drawable.stat_sys_data_connected).setOngoing(true).build()
    private fun notify(s:String){getSystemService(NotificationManager::class.java).notify(1001,n(s))}
    override fun onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy()}
    override fun onBind(i:Intent?)=null
}