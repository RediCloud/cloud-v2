package dev.redicloud.repository.node

import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlin.time.Duration.Companion.seconds

class NodeRepository(databaseConnection: DatabaseConnection, serviceId: ServiceId, packetManager: PacketManager)
    : ServiceRepository<CloudNode>(databaseConnection, serviceId, ServiceType.NODE, packetManager) {

    suspend fun getNode(serviceId: ServiceId): CloudNode? {
        if (serviceId.type != ServiceType.NODE) return null
        return getService(serviceId) as CloudNode?
    }

    suspend fun existsNode(serviceId: ServiceId): Boolean {
        if (serviceId.type != ServiceType.NODE) return false
        return existsService<CloudNode>(serviceId)
    }

    suspend fun updateNode(cloudNode: CloudNode): CloudNode
        = updateService(cloudNode) as CloudNode

    suspend fun createNode(cloudNode: CloudNode): CloudNode
        = createService(cloudNode) as CloudNode


    suspend fun getMasterNode(): CloudNode?
        = getConnectedNodes().firstOrNull { it.master }

    suspend fun getConnectedNodes(): List<CloudNode> =
        getConnectedIds().filter { serviceId.type == ServiceType.NODE }.mapNotNull { getNode(it) }

    suspend fun getRegisteredNodes(): List<CloudNode> =
        getRegisteredIds().filter { serviceId.type == ServiceType.NODE }.mapNotNull { getNode(it) }

}