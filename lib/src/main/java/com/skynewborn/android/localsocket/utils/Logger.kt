package com.skynewborn.android.localsocket.utils

import android.util.Log

internal object Logger {
    private const val TAG = "LocalSocket"
    
    private fun formatLog(message: String, category: String?): String {
        if (category?.isNotEmpty() == true) {
            return "$category: $message"
        }
        return message
    }
    
    fun v(message: String, category: String? = null): Int {
        return Log.v(TAG, formatLog(message, category))
    }
    
    fun d(message: String, category: String? = null): Int {
        return Log.d(TAG, formatLog(message, category))
    }
    
    fun i(message: String, category: String? = null): Int {
        return Log.i(TAG, formatLog(message, category))
    }
    
    fun w(message: String, category: String? = null): Int {
        return Log.w(TAG, formatLog(message, category))
    }
    
    fun e(message: String, category: String? = null): Int {
        return Log.e(TAG, formatLog(message, category))
    }
}