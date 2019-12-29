package com.example.androidclient.data

import com.android.volley.toolbox.HurlStack
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SelfSignedManager {
    fun makeHurlStack(): HurlStack {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<X509Certificate>,
                authType: String
            ) {
            }

            override fun checkServerTrusted(
                chain: Array<X509Certificate>,
                authType: String
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        })
        val sslContext: SSLContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, null)
        }
        val sslSocketFactory = sslContext.socketFactory
        return HurlStack(null, sslSocketFactory)
    }
}