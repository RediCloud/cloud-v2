package dev.redicloud.utils

import io.ktor.client.request.*
import io.ktor.http.*
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

suspend fun isValidUrl(url: String?): Boolean {
    if (url == null) return false
    return try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        val responseCode = connection.responseCode
        connection.disconnect()
        if (responseCode == HttpURLConnection.HTTP_BAD_METHOD) {
            return httpClient.get { url(url) }.status.isSuccess()
        }
        responseCode == HttpURLConnection.HTTP_OK
    }catch (e: Exception){
        false
    }
}

val URL.fileName: String
    get() {
        val lastIndexOf = path.removeSuffix("/").lastIndexOf("/")
        return if (lastIndexOf >= 0) {
            path.removeSuffix("/").substring(lastIndexOf + 1)
        } else {
            path.removeSuffix("/")
        }
    }
