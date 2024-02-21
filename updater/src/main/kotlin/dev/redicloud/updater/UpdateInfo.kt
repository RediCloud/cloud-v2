package dev.redicloud.updater

data class UpdateInfo(
    val version: String,
    val branch: String,
    val build: String
)