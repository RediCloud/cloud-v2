package dev.redicloud.repository.server

import dev.redicloud.api.repositories.service.server.CloudServerState
import dev.redicloud.api.repositories.service.server.ICloudServer
import dev.redicloud.repository.service.CloudService
import dev.redicloud.repository.service.ServiceSessions
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.utils.service.ServiceId
import java.util.UUID

abstract class CloudServer(
    serviceId: ServiceId,
    override var configurationTemplate: ConfigurationTemplate,
    override val id: Int,
    override var hostNodeId: ServiceId,
    serviceSessions: ServiceSessions,
    override var hidden: Boolean,
    override var state: CloudServerState = CloudServerState.UNKNOWN,
    override var port: Int = -1,
    override var maxPlayers: Int = -1,
    override var connectedPlayers: MutableList<UUID>
) : CloudService(
    serviceId,
    "${configurationTemplate.name}${configurationTemplate.serverSplitter}$id",
    serviceSessions
), ICloudServer {

    override fun unregisterAfterDisconnect(): Boolean {
        return super<CloudService>.unregisterAfterDisconnect() && !configurationTemplate.static
    }

}