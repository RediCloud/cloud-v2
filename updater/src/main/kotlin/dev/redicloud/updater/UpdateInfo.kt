package dev.redicloud.updater

data class UpdateInfo(
    val version: String,
    val branch: String,
    val build: String,
    val oldVersion: String,
    val oldBranch: String,
    val oldBuild: String
)