package dev.redicloud.utils

fun getGithubBranch(): String =
    System.getProperty("redicloud.git.branch", "main")

fun getGithubRepository(): String =
    System.getProperty("redicloud.git.repository", "cloud-v2")

fun getGithubUser(): String =
    System.getProperty("redicloud.git.user", "RediCloud")

fun getGithubCommitHash(): String =
    System.getProperty("redicloud.git.commit", "HEAD")

fun getRawUserContentUrl(): String =
    "https://raw.githubusercontent.com/${getGithubUser()}/${getGithubRepository()}/${getGithubBranch()}"
