package com.skynewborn.android.localsocket

class Api {

    /**
     * A native method that is implemented by the 'localsocket' native library,
     * which is packaged with this application.
     */
    external fun sendMessage(serviceId: String, message: String): String

    companion object {
        // Used to load the 'localsocket' library on application startup.
        init {
            System.loadLibrary("localsocket")
        }
    }
}