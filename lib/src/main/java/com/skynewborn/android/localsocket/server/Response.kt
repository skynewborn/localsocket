package com.skynewborn.android.localsocket.server

import org.json.JSONException
import org.json.JSONObject

private const val KEY_ID = "id"
private const val KEY_DATA = "data"
private const val KEY_CODE = "code"
private const val KEY_ERROR = "error"

internal class Response {
    private val json: JSONObject
    val serviceId: String
        get() = json.optString(KEY_ID, "")
    val code: Int
        get() = json.optInt(KEY_CODE, 1)
    val data: String?
        get() = json.optString(KEY_DATA)
    val error: String?
        get() = json.optString(KEY_ERROR)
    
    constructor(response: String) {
        json = try {
            JSONObject(response)
        } catch (e: JSONException) {
            JSONObject()
        }
    }
    
    constructor(id: String, data: String?, error: Throwable? = null) {
        json = JSONObject()
        json.put(KEY_ID, id)
        if (data == null) {
            json.put(KEY_CODE, 1)
            json.put(KEY_ERROR, error?.message ?: "Unknown error!")
        } else {
            json.put(KEY_CODE, 0)
            json.put(KEY_DATA, data)
        }
    }

    override fun toString(): String {
        return json.toString()
    }
}