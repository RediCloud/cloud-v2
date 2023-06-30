package dev.redicloud.utils

fun getGithubBranche(): String =
    System.getProperty("redicloud.git.branch", "master")

fun getGithubRepository(): String =
    System.getProperty("redicloud.git.repository", "cloud-v2")

fun getGithubUser(): String =
    System.getProperty("redicloud.git.user", "RediCloud")

fun getGithubCommitHash(): String =
    System.getProperty("redicloud.git.commit", "HEAD")

fun getRawUserContentUrl(): String =
    "https://raw.githubusercontent.com/${getGithubUser()}/${getGithubRepository()}/${getGithubBranche()}"