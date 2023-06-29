package dev.redicloud.repository.template.configuration

import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.versions.JavaVersion
import java.util.*

data class ConfigurationTemplate(
    val uniqueId: UUID = UUID.randomUUID(),
    var name: String,
    val programmArguments: MutableList<String> = mutableListOf(),
    val jvmArguments: MutableList<String> = mutableListOf(),
    val environments: MutableMap<String, String> = mutableMapOf(),
    var javaCommand: String = "java",
    var maxMemory: Long = 750,
    val fileTemplateIds: MutableList<UUID> = mutableListOf(),
    val nodeIds: MutableList<ServiceId> = mutableListOf(),
    var minStartedServices: Int = 0,
    var maxStartedServices: Int = -1,
    var minStartedServicesPerNode: Int = 0,
    var maxStartedServicesPerNode: Int = -1,
    var percentToStartNewService: Double = 90.0,
    var serverSplitter: String = "-",
    var fallbackServer: Boolean = false,
    var startPriority: Int = if(fallbackServer) 0 else 50,
    var serverVersionId: UUID,
    val static: Boolean = false,
    var startPort: Int = 40000
) : Comparable<ConfigurationTemplate> {

    override fun compareTo(other: ConfigurationTemplate): Int
        = startPriority.compareTo(other.startPriority)

}