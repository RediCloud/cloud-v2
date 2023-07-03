package dev.redicloud.repository.server

import dev.redicloud.api.server.events.server.CloudServerConnectedEvent
import dev.redicloud.api.server.events.server.CloudServerUnregisteredEvent
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.event.EventManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.service.base.events.server.CloudServerRegisteredEvent
import dev.redicloud.service.base.events.server.CloudServerStateChangeEvent
import dev.redicloud.service.base.events.server.CloudServerDisconnectedEvent
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType

class ServerRepository(
    databaseConnection: DatabaseConnection,
    serviceId: ServiceId,
    packetManager: PacketManager,
    private val eventManager: EventManager
) : ServiceRepository<CloudServer>(databaseConnection, serviceId, ServiceType.SERVER, packetManager) {

    suspend fun getServer(serviceId: ServiceId): CloudServer? =
        getService(serviceId) as CloudServer?

    suspend fun updateServer(cloudServer: CloudServer): CloudServer {
        val oldServer = getServer(cloudServer.serviceId)
        val newServer = updateService(cloudServer) as CloudServer
        if (oldServer?.state != newServer.state) {
            eventManager.fireEvent(CloudServerStateChangeEvent(newServer.serviceId, newServer.state))
        }
        if (oldServer != null && oldServer.connected != newServer.connected) {
            if (newServer.connected) {
                eventManager.fireEvent(CloudServerConnectedEvent(newServer.serviceId))
            } else {
                eventManager.fireEvent(CloudServerDisconnectedEvent(newServer.serviceId))
            }
        }
        return newServer
    }

    suspend fun createServer(cloudServer: CloudServer): CloudServer {
        val newServer = createService(cloudServer) as CloudServer
        eventManager.fireEvent(CloudServerRegisteredEvent(newServer.serviceId))
        return newServer
    }

    suspend fun deleteServer(cloudServer: CloudServer) {
        deleteService(cloudServer)
        eventManager.fireEvent(CloudServerUnregisteredEvent(cloudServer.serviceId))
    }

    suspend fun getConnectedServers(): List<CloudServer> =
        getConnectedServices().filter { it.serviceId.type == ServiceType.SERVER }.toList() as List<CloudServer>

    suspend fun getRegisteredServers(): List<CloudServer> =
        getRegisteredServices().filter { it.serviceId.type == ServiceType.SERVER }.toList() as List<CloudServer>

}