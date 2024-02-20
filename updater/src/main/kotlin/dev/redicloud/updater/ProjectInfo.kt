package dev.redicloud.updater

data class ProjectInfo(
    val project: String,
    val latest_build: Int,
    val builds: List<Int>
)