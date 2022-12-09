package com.skynewborn.android.localsocket.demo

import com.skynewborn.android.localsocket.handler.BaseRequestHandler
import com.skynewborn.android.localsocket.handler.IRequestHandler

class TestHandler : BaseRequestHandler() {
    override fun handleRequest(request: String, callback: IRequestHandler.Callback) {
        callback.onHandled("Echo: $request")
    }
    
    companion object {
        const val ID = "test"
    }
}