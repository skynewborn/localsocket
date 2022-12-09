package com.skynewborn.android.localsocket.server

import org.json.JSONException
import org.json.JSONObject

private const val KEY_ID = "id"
private const val KEY_DATA = "data"

internal class Request {
    private var json: JSONObject
    val serviceId: String
        get() = json.optString(KEY_ID)
    val data: String
        get() = json.optString(KEY_DATA)

    constructor(id: String, data: String) {
        json = JSONObject()
        try {
            json.put(KEY_ID, id)
            json.put(KEY_DATA, data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    constructor(req: String) {
        json = try {
            JSONObject(req)
        } catch (e: JSONException) {
            e.printStackTrace()
            JSONObject()
        }
    }

    override fun toString(): String {
        return json.toString()
    }
}