package dev.redicloud.api.service.server

import dev.redicloud.api.template.configuration.ICloudConfigurationTemplate
import dev.redicloud.api.service.ICloudService
import dev.redicloud.api.service.ServiceId
import java.util.*

interface ICloudServer : ICloudService {

    val configurationTemplate: ICloudConfigurationTemplate
    val id: Int
    val hostNodeId: ServiceId
    var hidden: Boolean
    var state: CloudServerState
    val port: Int
    val maxPlayers: Int
    val connectedPlayers: MutableList<UUID>

}