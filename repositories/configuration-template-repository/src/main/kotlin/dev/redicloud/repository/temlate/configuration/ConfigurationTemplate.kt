package dev.redicloud.repository.temlate.configuration

import java.util.*

data class ConfigurationTemplate(
    val uniqueId: UUID = UUID.randomUUID(),
    var name: String,
    val programmArguments: MutableList<String> = mutableListOf(),
    val jvmArguments: MutableList<String> = mutableListOf(),
    var maxMemory: Long = 750,
    val fileTemplates: MutableList<UUID> = mutableListOf(),
    var minStartedServicesGlobally: Int = 0,
    var maxStartedServicesGlobally: Int = -1,
    var minStartedServicesPerNode: Int = 0,
    var maxStartedServicesPerNode: Int = -1,
    var percentToStartNewService: Double = 90.0
)