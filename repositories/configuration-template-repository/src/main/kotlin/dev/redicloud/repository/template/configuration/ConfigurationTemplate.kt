package dev.redicloud.repository.template.configuration

import dev.redicloud.utils.ProcessConfiguration
import dev.redicloud.utils.service.ServiceId
import java.util.*

class ConfigurationTemplate(
    val uniqueId: UUID = UUID.randomUUID(),
    var name: String,
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
    var startPriority: Int = if (fallbackServer) 0 else 50,
    var serverVersionId: UUID?,
    var static: Boolean = false,
    var startPort: Int = 40000,
    var joinPermission: String? = null,
    var maxPlayers: Int = 50,
    jvmArguments: MutableList<String> = mutableListOf(),
    environmentVariables: MutableMap<String, String> = mutableMapOf(),
    processArguments: MutableList<String> = mutableListOf(),
    defaultFiles: MutableMap<String, String> = mutableMapOf(),
    fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
) : ProcessConfiguration(
    jvmArguments,
    environmentVariables,
    processArguments,
    defaultFiles,
    fileEdits
), Comparable<ConfigurationTemplate> {

    override fun compareTo(other: ConfigurationTemplate): Int = startPriority.compareTo(other.startPriority)

}