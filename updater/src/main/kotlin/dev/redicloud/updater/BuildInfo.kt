package dev.redicloud.updater

data class BuildInfo(
    val branch: String,
    val build: Int,
    val version: String,
    val workflowId: Long,
    val stored: Boolean
)