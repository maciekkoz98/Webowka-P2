package com.example.androidclient.data

import android.content.Context
import com.android.volley.toolbox.HurlStack
import com.example.androidclient.R
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class SelfSignedManager {
    fun makeHurlStack(context: Context): HurlStack {
//        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
////        val caInput: InputStream = BufferedInputStream(FileInputStream(R.raw.server))
//        val caInput: InputStream = context.getResources().openRawResource(R.raw.server)
//        val ca: X509Certificate = caInput.use {
//            cf.generateCertificate(it) as X509Certificate
//        }
//        val keyStoreType = KeyStore.getDefaultType()
//        val keyStore = KeyStore.getInstance(keyStoreType).apply {
//            load(null, null)
//            setCertificateEntry("ca", ca)
//        }
//        val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
//        val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
//            init(keyStore)
//        }
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