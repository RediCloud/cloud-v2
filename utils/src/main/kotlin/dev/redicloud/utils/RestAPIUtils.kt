package dev.redicloud.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

fun getAPIUrl(): String {
    return "${getRootAPIUrl()}/files/$BRANCH/$BUILD"
}

fun getRootAPIUrl(): String {
    return "https://api.redicloud.dev/v2"
}

suspend fun getAPIUrlOrFallback(): String {
    if (isValidUrl("${getAPIUrl()}/status")) return getAPIUrl()
    return getRawUserContentUrl()
}

suspend fun getTextOfAPIWithFallback(path: String): String {
    if (isValidUrl("${getAPIUrl()}/status")) {
        val response = httpClient.get {
            url("${getAPIUrl()}/$path")
        }
        if (response.status.isSuccess()) return response.bodyAsText()
    }
    return httpClient.get { url("${getRawUserContentUrl()}/$path") }.bodyAsText()
}