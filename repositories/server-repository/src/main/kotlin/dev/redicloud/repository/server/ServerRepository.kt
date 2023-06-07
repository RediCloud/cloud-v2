package dev.redicloud.repository.server

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType

class ServerRepository(databaseConnection: DatabaseConnection, serviceId: ServiceId, packetManager: PacketManager)
    : ServiceRepository<CloudServer>(databaseConnection, serviceId, ServiceType.SERVER, packetManager) {


    suspend fun getServer(serviceId: ServiceId): CloudServer? =
        get(serviceId.id.toString())

    suspend fun getServers(): List<CloudServer> =
        getAll()

}