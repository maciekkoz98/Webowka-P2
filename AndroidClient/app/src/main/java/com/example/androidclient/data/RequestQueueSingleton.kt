package com.example.androidclient.data

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley

class RequestQueueSingleton constructor(context: Context, manager: HurlStack) {
    companion object {
        @Volatile
        private var INSTANCE: RequestQueueSingleton? = null

        fun getInstance(context: Context, manager: HurlStack) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: RequestQueueSingleton(context, manager).also {
                INSTANCE = it
            }
        }
    }

    val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext, manager)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }
}