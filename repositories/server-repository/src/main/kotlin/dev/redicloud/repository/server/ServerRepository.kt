package dev.redicloud.repository.server

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.utils.ServiceId
import dev.redicloud.utils.ServiceType

class ServerRepository(databaseConnection: DatabaseConnection, serviceId: ServiceId)
    : ServiceRepository<CloudServer>(databaseConnection, serviceId, ServiceType.SERVER) {


    suspend fun getServer(serviceId: ServiceId): CloudServer? =
        get(serviceId.id.toString())

    suspend fun getServers(): List<CloudServer> =
        getAll()

}