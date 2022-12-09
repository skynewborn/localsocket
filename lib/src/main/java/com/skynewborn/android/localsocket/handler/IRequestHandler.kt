package com.skynewborn.android.localsocket.handler

import android.content.Context

/**
 * Interface of handlers for local socket requests.
 * Requests and responses are UTF8-encoded strings, so handler is responsible for
 * serialization/deserialization during the process.
 */
interface IRequestHandler {
    interface Callback {
        fun onHandled(response: String)
        fun onError(error: Throwable)
    }
    
    fun create(context: Context)
    fun destroy(context: Context)
    fun start()
    fun stop()
    fun handleRequest(request: String, callback: Callback)
}