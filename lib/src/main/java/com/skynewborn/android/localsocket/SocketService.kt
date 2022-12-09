package com.skynewborn.android.localsocket

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.skynewborn.android.localsocket.server.Server
import com.skynewborn.android.localsocket.utils.Logger

class SocketService : Service() {
    private lateinit var socketServer: Server
    
    override fun onCreate() {
        super.onCreate()
        socketServer = Server(this, SERVICE_NAME)
        Logger.i("Service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.i("Service start.")
        socketServer.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        socketServer.close()
        Logger.i("Service destroyed.")
    }
    
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
    
    companion object {
        const val SERVICE_NAME = "com.skynewborn.android.localsocket"
        
        fun sendRequest(request: String): String {
            return Server.sendRequest(SERVICE_NAME, request)
        }
        
        fun sendRequest(serviceId: String, data: String): String {
            return Server.sendRequest(SERVICE_NAME, serviceId, data)
        }
    }
}