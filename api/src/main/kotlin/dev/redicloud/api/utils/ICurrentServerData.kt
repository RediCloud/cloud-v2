package dev.redicloud.api.utils

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.CloudServerState
import java.util.*

interface ICurrentServerData {

    val serviceId: ServiceId
    val name: String
    val id: Int
    val maxPlayers: Int
    val connectedPlayers: List<UUID>
    val state: CloudServerState
    val configurationTemplateName: String
    val serverVersionName: String

}