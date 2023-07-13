package dev.redicloud.utils

fun getAPIUrl(): String {
    return "https://api.redicloud.dev/build/$PROJECT_INFO/$BUILD_NUMBER"
}

suspend fun getAPIUrlOrFallback(): String {
    if (isValidUrl("${getAPIUrl()}/status")) return getAPIUrl()
    return getRawUserContentUrl()
}