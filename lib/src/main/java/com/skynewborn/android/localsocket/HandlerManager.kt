package com.skynewborn.android.localsocket

import android.content.Context
import com.skynewborn.android.localsocket.handler.IRequestHandler
import com.skynewborn.android.localsocket.utils.SingletonHolder
import java.util.concurrent.atomic.AtomicBoolean

class HandlerManager private constructor(context: Context) {
    private val context = context.applicationContext
    private val handlerMap = HashMap<String, IRequestHandler>()
    private val openFlag = AtomicBoolean(false)
    
    fun add(id: String, handler: IRequestHandler) {
        handlerMap[id] = handler
    }
    
    fun remove(id: String) {
        handlerMap.remove(id)
    }
    
    fun contains(id: String): Boolean {
        return handlerMap.contains(id)
    }
    
    fun get(id: String): IRequestHandler? {
        return handlerMap[id]
    }
    
    fun clear() {
        handlerMap.clear()
    }
    
    fun open() = synchronized(openFlag) {
        if (!openFlag.get()) {
            handlerMap.forEach { (_, handler) ->
                try {
                    handler.create(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            openFlag.set(true)
        }
    }
    
    fun close() = synchronized(openFlag) {
        if (openFlag.get()) {
            handlerMap.forEach { (_, handler) ->
                try {
                    handler.destroy(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            openFlag.set(false)
        }
    }
    
    fun start(id: String) {
        val handler = get(id) ?: return
        synchronized(openFlag) {
            if (openFlag.get()) {
                try {
                    handler.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun stop(id: String) {
        val handler = get(id) ?: return
        synchronized(openFlag) {
            if (openFlag.get()) {
                try {
                    handler.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun startAll() = synchronized(openFlag) {
        if (openFlag.get()) {
            handlerMap.forEach { (_, handler) ->
                try {
                    handler.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopAll() = synchronized(openFlag) {
        if (openFlag.get()) {
            handlerMap.forEach { (_, handler) ->
                try {
                    handler.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    companion object : SingletonHolder<HandlerManager, Context>(::HandlerManager) {
        private const val TAG = "HandlerManager"
    }
}