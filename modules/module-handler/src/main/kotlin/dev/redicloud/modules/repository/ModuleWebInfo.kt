package dev.redicloud.modules.repository

data class ModuleWebInfo(
    val id: String,
    val description: String,
    val author: String,
    val website: String,
    val versions: MutableMap<String, String>
)