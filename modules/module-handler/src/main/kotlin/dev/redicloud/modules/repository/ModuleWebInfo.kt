package dev.redicloud.modules.repository

data class ModuleWebInfo(
    val id: String,
    val description: String,
    val authors: List<String>,
    val website: String,
    val versions: List<String>
)