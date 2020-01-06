package com.example.androidclient.data

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.*
import kotlin.math.min

class FileSendRequestVolley(
    url: String,
    private val responseListener: Response.Listener<NetworkResponse>,
    errorListener: Response.ErrorListener
) : Request<NetworkResponse>(Method.POST, url, errorListener) {

    private val lineEnd = "\r\n"
    private val twoHyphens = "--"
    private val boundary = ":_+"

    var dataMap: Map<String, DataPart>? = null
    var paramsMap: Map<String, String>? = null

    override fun parseNetworkResponse(response: NetworkResponse?): Response<NetworkResponse> {
        return try {
            Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            Response.error(ParseError(e))
        }
    }

    override fun deliverResponse(response: NetworkResponse?) {
        responseListener.onResponse(response)
    }

    override fun getBodyContentType(): String {
        return "multipart/form-data;boundary=$boundary"
    }

    override fun getBody(): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val dataOutputStream = DataOutputStream(byteArrayOutputStream)
        try {
            val parameters = paramsMap
            if (parameters != null && parameters.isNotEmpty()) {
                textParse(dataOutputStream, parameters)
            }

            val byteData = dataMap
            if (byteData != null && byteData.isNotEmpty()) {
                dataParse(dataOutputStream, byteData)
            }

            dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
            return byteArrayOutputStream.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun textParse(
        dataOutputStream: DataOutputStream,
        parameters: Map<String, String>
    ) {
        try {
            parameters.forEach { (key, value) ->
                buildTextPart(dataOutputStream, key, value)
            }
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException("Encoding not supported: utf-8", e)
        }
    }

    private fun dataParse(dataOutputStream: DataOutputStream, byteData: Map<String, DataPart>) {
        byteData.forEach { (key, value) -> buildByteData(dataOutputStream, key, value) }
    }

    private fun buildTextPart(
        dataOutputStream: DataOutputStream,
        paramName: String,
        paramValue: String
    ) {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd)
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"$paramName\"$lineEnd")
        dataOutputStream.writeBytes(lineEnd)
        dataOutputStream.writeBytes(paramValue + lineEnd)
    }

    private fun buildByteData(
        dataOutputStream: DataOutputStream,
        fileName: String,
        dataFile: DataPart
    ) {
        println("Nazwa pliku w buildByteData: ${dataFile.filename}")
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd)
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"$fileName\"; filename=\"${dataFile.filename}\"$lineEnd")
        dataOutputStream.writeBytes("Content-Type: " + dataFile.mimeType + lineEnd)
        dataOutputStream.writeBytes(lineEnd)

        val byteInputStream = ByteArrayInputStream(dataFile.content)
        var byteAvailable = byteInputStream.available()
        val maxBuffer = 1024 * 1024
        var bufferSize = min(byteAvailable, maxBuffer)
        val buffer = ByteArray(bufferSize)
        var bytesRead = byteInputStream.read(buffer, 0, bufferSize)
        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize)
            byteAvailable = byteInputStream.available()
            bufferSize = min(byteAvailable, maxBuffer)
            bytesRead = byteInputStream.read(buffer, 0, bufferSize)
        }
        dataOutputStream.writeBytes(lineEnd)
    }

    data class DataPart(val filename: String?, val content: ByteArray, val mimeType: String)
}