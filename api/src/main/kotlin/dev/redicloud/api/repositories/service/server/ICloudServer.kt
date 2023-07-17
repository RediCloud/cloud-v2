package dev.redicloud.api.repositories.service.server

import dev.redicloud.api.repositories.template.configuration.ICloudConfigurationTemplate
import dev.redicloud.api.repositories.service.ICloudService
import dev.redicloud.utils.service.ServiceId
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