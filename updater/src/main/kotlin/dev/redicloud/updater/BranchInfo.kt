package dev.redicloud.updater

data class BranchInfo(
    val branch: String,
    val builds: List<Int>,
    val buildTasks: List<String>
)