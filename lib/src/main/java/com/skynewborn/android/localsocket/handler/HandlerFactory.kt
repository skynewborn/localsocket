package com.skynewborn.android.localsocket.handler

import android.content.Context
import com.skynewborn.android.localsocket.HandlerManager
import com.skynewborn.android.localsocket.utils.SingletonHolder

internal class HandlerFactory private constructor(context: Context) {
    private val handlerManager = HandlerManager.getInstance(context)
    
    fun getHandler(id: String): IRequestHandler? {
        return handlerManager.get(id)
    }
    
    companion object : SingletonHolder<HandlerFactory, Context>(::HandlerFactory) {
    }
}