package dev.redicloud.api.template.configuration

import dev.redicloud.api.utils.ProcessConfiguration
import dev.redicloud.api.service.ServiceId
import java.util.UUID

interface ICloudConfigurationTemplate : Comparable<ICloudConfigurationTemplate>, ProcessConfiguration {

    val uniqueId: UUID
    val name: String
    val maxMemory: Long
    val fileTemplateIds: MutableList<UUID>
    val nodeIds: MutableList<ServiceId>
    var minStartedServices: Int
    var maxStartedServices: Int
    var minStartedServicesPerNode: Int
    var maxStartedServicesPerNode: Int
    var percentToStartNewService: Double
    var serverSplitter: String
    var fallbackServer: Boolean
    var startPriority: Int
    var serverVersionId: UUID?
    var static: Boolean
    var startPort: Int
    var joinPermission: String?
    var maxPlayers: Int
    var timeAfterStopUselessServer: Long

}