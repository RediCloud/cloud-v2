package dev.redicloud.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Fetches a text file from the GitHub repository's raw content URL.
 *
 * Used by version repositories to load api-files (server-versions.json, etc.)
 * from the repository at the configured branch.
 */
suspend fun getTextFromGitHub(path: String): String {
    val response = httpClient.get {
        url("${getRawUserContentUrl()}/$path")
    }
    check(response.status.isSuccess()) { "Failed to fetch $path from GitHub: HTTP ${response.status}" }
    return response.bodyAsText()
}

/**
 * Backward-compatible alias for [getTextFromGitHub].
 *
 * Previously tried api.redicloud.dev first with a GitHub fallback.
 * Now goes directly to GitHub raw content.
 */
@Deprecated(
    message = "Use getTextFromGitHub() directly",
    replaceWith = ReplaceWith("getTextFromGitHub(path)")
)
suspend fun getTextOfAPIWithFallback(path: String): String = getTextFromGitHub(path)
