package dev.redicloud.updater

data class ProjectInfo(
    val branch: String,
    val builds: List<Int>,
    val buildTasks: List<String>
)