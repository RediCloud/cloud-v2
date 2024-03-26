package dev.redicloud.repository.template.configuration

import dev.redicloud.api.template.configuration.ICloudConfigurationTemplate
import dev.redicloud.cache.IClusterCacheObject
import dev.redicloud.api.service.ServiceId
import java.util.*
import kotlin.time.Duration.Companion.seconds

class ConfigurationTemplate(
    override val uniqueId: UUID = UUID.randomUUID(),
    override var name: String,
    override var maxMemory: Long = 750,
    override val fileTemplateIds: MutableList<UUID> = mutableListOf(),
    override val nodeIds: MutableList<ServiceId> = mutableListOf(),
    override var minStartedServices: Int = 0,
    override var maxStartedServices: Int = -1,
    override var minStartedServicesPerNode: Int = 0,
    override var maxStartedServicesPerNode: Int = -1,
    override var percentToStartNewService: Double = 90.0,
    override var serverSplitter: String = "-",
    override var fallbackServer: Boolean = false,
    override var startPriority: Int = if (fallbackServer) 0 else 50,
    override var serverVersionId: UUID?,
    override var static: Boolean = false,
    override var startPort: Int = -1,
    override var joinPermission: String? = null,
    override var maxPlayers: Int = 50,
    override var timeAfterStopUselessServer: Long = 120.seconds.inWholeMilliseconds,
    override val jvmArguments: MutableList<String> = mutableListOf(),
    override val environmentVariables: MutableMap<String, String> = mutableMapOf(),
    override val programParameters: MutableList<String> = mutableListOf(),
    override val defaultFiles: MutableMap<String, String> = mutableMapOf(),
    override val fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
) : ICloudConfigurationTemplate, IClusterCacheObject, Comparable<ICloudConfigurationTemplate> {

    override fun compareTo(other: ICloudConfigurationTemplate): Int = startPriority.compareTo(other.startPriority)

    fun copy(name: String): ICloudConfigurationTemplate {
        return ConfigurationTemplate(
            uniqueId = UUID.randomUUID(),
            name = name,
            maxMemory = maxMemory,
            fileTemplateIds = fileTemplateIds,
            nodeIds = nodeIds,
            minStartedServices = minStartedServices,
            maxStartedServices = maxStartedServices,
            minStartedServicesPerNode = minStartedServicesPerNode,
            maxStartedServicesPerNode = maxStartedServicesPerNode,
            percentToStartNewService = percentToStartNewService,
            serverSplitter = serverSplitter,
            fallbackServer = fallbackServer,
            startPriority = startPriority,
            serverVersionId = serverVersionId,
            static = static,
            startPort = startPort,
            joinPermission = joinPermission,
            maxPlayers = maxPlayers,
            timeAfterStopUselessServer = timeAfterStopUselessServer,
            jvmArguments = jvmArguments,
            environmentVariables = environmentVariables,
            programParameters = programParameters,
            defaultFiles = defaultFiles,
            fileEdits = fileEdits
        )
    }

}