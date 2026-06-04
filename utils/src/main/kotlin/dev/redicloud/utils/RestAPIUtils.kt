package dev.redicloud.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Fetches a text file from the GitHub repository's raw content URL.
 *
 * Tries the commit-pinned URL first. If that returns a 404 (e.g. local
 * dev builds with unpushed commits), falls back to the branch-based URL.
 *
 * Used by version repositories to load api-files (server-versions.json, etc.)
 * from the repository.
 */
suspend fun getTextFromGitHub(path: String): String {
    val baseUrl = "https://raw.githubusercontent.com/${getGithubUser()}/${getGithubRepository()}"
    val commitRef = GIT.takeIf { it != "unknown" }
    val branchRef = getGithubBranch()

    if (commitRef != null) {
        val response = httpClient.get { url("$baseUrl/$commitRef/$path") }
        if (response.status.isSuccess()) return response.bodyAsText()
    }

    val fallbackResponse = httpClient.get { url("$baseUrl/$branchRef/$path") }
    check(fallbackResponse.status.isSuccess()) {
        "Failed to fetch $path from GitHub: HTTP ${fallbackResponse.status}"
    }
    return fallbackResponse.bodyAsText()
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
