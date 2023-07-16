package dev.redicloud.repository.server

import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.repository.service.CloudService
import dev.redicloud.repository.service.ServiceSessions
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.utils.service.ServiceId
import java.util.UUID

abstract class CloudServer(
    serviceId: ServiceId,
    var configurationTemplate: ConfigurationTemplate,
    val id: Int,
    var hostNodeId: ServiceId,
    serviceSessions: ServiceSessions,
    var hidden: Boolean,
    var state: CloudServerState = CloudServerState.UNKNOWN,
    var port: Int = -1,
    var maxPlayers: Int = -1,
    var connectedPlayers: MutableList<UUID>
) : CloudService(serviceId, "${configurationTemplate.name}${configurationTemplate.serverSplitter}$id", serviceSessions) {

    override fun unregisterAfterDisconnect(): Boolean {
        return super.unregisterAfterDisconnect() && !configurationTemplate.static
    }

}