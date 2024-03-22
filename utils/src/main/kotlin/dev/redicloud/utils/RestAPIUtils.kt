package dev.redicloud.utils

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
        val restResponse = khttp.get("${getAPIUrl()}/$path")
        if (restResponse.statusCode == 200) return restResponse.text
    }
    return khttp.get("${getRawUserContentUrl()}/$path").text
}