package dev.redicloud.utils

import java.net.HttpURLConnection
import java.net.URL

suspend fun URL.isValid(): Boolean {
    return try {
        val connection = openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        val responseCode = connection.responseCode
        connection.disconnect()
        responseCode == HttpURLConnection.HTTP_OK
    }catch (e: Exception){
        false
    }
}

suspend fun isValidUrl(url: String): Boolean {
    return try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        val responseCode = connection.responseCode
        connection.disconnect()
        responseCode == HttpURLConnection.HTTP_OK
    }catch (e: Exception){
        false
    }
}