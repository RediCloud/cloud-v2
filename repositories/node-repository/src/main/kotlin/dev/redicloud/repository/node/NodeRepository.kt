package dev.redicloud.repository.node

import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.utils.ServiceId
import dev.redicloud.utils.ServiceType

class NodeRepository(databaseConnection: DatabaseConnection, serviceId: ServiceId)
    : ServiceRepository<CloudNode>(databaseConnection, serviceId, ServiceType.NODE) {

    suspend fun getNode(serviceId: ServiceId): CloudNode? {
        if (serviceId.type != ServiceType.NODE) return null
        return get(serviceId.id.toString())
    }

    suspend fun existsNode(serviceId: ServiceId): Boolean {
        if (serviceId.type != ServiceType.NODE) return false
        return exists(serviceId.id.toString())
    }

}