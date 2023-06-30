package dev.redicloud.repository.server

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType

class ServerRepository(databaseConnection: DatabaseConnection, serviceId: ServiceId, packetManager: PacketManager)
    : ServiceRepository<CloudServer>(databaseConnection, serviceId, ServiceType.SERVER, packetManager) {

    suspend fun getServer(serviceId: ServiceId): CloudServer? =
        getService(serviceId) as CloudServer?

    suspend fun updateServer(cloudServer: CloudServer): CloudServer
        = updateService(cloudServer) as CloudServer

    suspend fun createServer(cloudServer: CloudServer): CloudServer
        = createService(cloudServer) as CloudServer

    suspend fun deleteServer(cloudServer: CloudServer)
        = deleteService(cloudServer)

    suspend fun getConnectedServers(): List<CloudServer> =
        getConnectedServices().filter { it.serviceId.type == ServiceType.SERVER }.toList() as List<CloudServer>

    suspend fun getRegisteredServers(): List<CloudServer> =
        getRegisteredServices().filter { it.serviceId.type == ServiceType.SERVER }.toList() as List<CloudServer>

}