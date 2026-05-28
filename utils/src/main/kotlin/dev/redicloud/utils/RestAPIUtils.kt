package dev.redicloud.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

const val ROOT_API_URL = "https://api.redicloud.dev/v2"

fun getAPIUrl(): String {
    return "$ROOT_API_URL/files/$BRANCH/$BUILD"
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
