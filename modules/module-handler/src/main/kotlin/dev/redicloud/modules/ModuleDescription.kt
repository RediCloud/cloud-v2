package dev.redicloud.modules

import dev.redicloud.api.service.ServiceType

data class ModuleDescription(
    val name: String,
    val id: String,
    val version: String,
    val description: String,
    val website: String? = null,
    val authors: List<String>,
    val mainClasses: HashMap<ServiceType, String>
)