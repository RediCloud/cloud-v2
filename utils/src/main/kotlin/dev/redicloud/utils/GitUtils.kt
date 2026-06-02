package dev.redicloud.utils

fun getGithubBranch(): String =
    System.getProperty("redicloud.git.branch", "main")

fun getGithubRepository(): String =
    System.getProperty("redicloud.git.repository", "cloud-v2")

fun getGithubUser(): String =
    System.getProperty("redicloud.git.user", "RediCloud")

fun getGithubCommitHash(): String =
    System.getProperty("redicloud.git.commit", "HEAD")

/**
 * Returns the GitHub raw content base URL, pinned to the current git commit hash when available.
 *
 * This ensures that fetched api-files (server-version-types.json, etc.) match the running binary
 * exactly, rather than potentially drifting when the branch moves forward.
 *
 * Falls back to the branch name when the commit hash is unavailable (e.g. local dev builds).
 */
fun getRawUserContentUrl(): String {
    val ref = GIT.takeIf { it != "unknown" } ?: getGithubBranch()
    return "https://raw.githubusercontent.com/${getGithubUser()}/${getGithubRepository()}/$ref"
}
