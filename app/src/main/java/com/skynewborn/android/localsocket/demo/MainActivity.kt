package com.skynewborn.android.localsocket.demo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.skynewborn.android.localsocket.Api
import com.skynewborn.android.localsocket.HandlerManager
import com.skynewborn.android.localsocket.SocketService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val nativeApi = Api()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var serviceIntent: Intent
    private lateinit var handlerManager: HandlerManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tvConsole = findViewById<TextView>(R.id.tvConsole)
        findViewById<Button>(R.id.btnJava).apply { 
            setOnClickListener { 
                executor.execute { 
                    val res = SocketService.sendRequest(TestHandler.ID, "Hello from Java!")
                    runOnUiThread { 
                        tvConsole.text = res
                    }
                }
            }
        }
        findViewById<Button>(R.id.btnNative).apply { 
            setOnClickListener { 
                executor.execute { 
                    val res = nativeApi.sendMessage(TestHandler.ID, "Hello from C++!")
                    runOnUiThread { 
                        tvConsole.text = res
                    }
                }
            }
        }

        serviceIntent = Intent(this, SocketService::class.java)
        handlerManager = HandlerManager.getInstance(this).apply { 
            add(TestHandler.ID, TestHandler())
            open()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerManager.close()
    }

    override fun onStart() {
        super.onStart()
        startService(serviceIntent)
        handlerManager.startAll()
    }

    override fun onStop() {
        super.onStop()
        stopService(serviceIntent)
        handlerManager.stopAll()
    }
}