package dev.redicloud.repository.server

import dev.redicloud.api.server.events.server.CloudServerConnectedEvent
import dev.redicloud.api.server.events.server.CloudServerUnregisteredEvent
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.event.EventManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.service.base.events.server.CloudServerDisconnectedEvent
import dev.redicloud.service.base.events.server.CloudServerRegisteredEvent
import dev.redicloud.service.base.events.server.CloudServerStateChangeEvent
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import org.redisson.api.RBucket

class ServerRepository(
    databaseConnection: DatabaseConnection,
    serviceId: ServiceId,
    packetManager: PacketManager,
    private val eventManager: EventManager
) : ServiceRepository<CloudServer>(databaseConnection, serviceId, serviceId.type, packetManager) {

    private fun <T : CloudServer> getBase(serviceId: ServiceId): RBucket<T> =
        getUnsafeHandle<T>("service:${serviceId.type.name.lowercase()}:${serviceId.id}", true)

    suspend fun <T : CloudServer> getServer(serviceId: ServiceId): T? =
        getBase<T>(serviceId).get()

    suspend fun getMinecraftServer(serviceId: ServiceId): CloudMinecraftServer? =
        getServer(serviceId)

    suspend fun getProxyServer(serviceId: ServiceId): CloudProxyServer? =
        getServer(serviceId)

    suspend fun <T : CloudServer> updateServer(cloudServer: T): T {
        val oldServer = getServer<T>(cloudServer.serviceId)
        getBase<CloudServer>(cloudServer.serviceId).set(cloudServer)
        if (oldServer?.state != cloudServer.state) {
            eventManager.fireEvent(CloudServerStateChangeEvent(cloudServer.serviceId, cloudServer.state))
        }
        if (oldServer != null && oldServer.connected != cloudServer.connected) {
            if (cloudServer.connected) {
                eventManager.fireEvent(CloudServerConnectedEvent(cloudServer.serviceId))
            } else {
                eventManager.fireEvent(CloudServerDisconnectedEvent(cloudServer.serviceId))
            }
        }
        return cloudServer
    }

    fun <T : CloudServer> createServer(cloudServer: T): T {
        getBase<T>(cloudServer.serviceId).set(cloudServer)
        eventManager.fireEvent(CloudServerRegisteredEvent(cloudServer.serviceId))
        return cloudServer
    }


    suspend fun <T : CloudServer> deleteServer(cloudServer: T) {
        getBase<T>(cloudServer.serviceId).delete()
        eventManager.fireEvent(CloudServerUnregisteredEvent(cloudServer.serviceId))
    }

    suspend fun getConnectedServers(): List<CloudServer> =
        getConnectedServices().filter { it.serviceId.type.isServer() }.toList() as List<CloudServer>

    suspend fun getRegisteredServers(): List<CloudServer> =
        getRegisteredServices().filter { it.serviceId.type.isServer() }.toList() as List<CloudServer>

}