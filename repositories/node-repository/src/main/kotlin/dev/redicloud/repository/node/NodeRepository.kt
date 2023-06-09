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
        return get(serviceId.id.toString())
    }

    suspend fun existsNode(serviceId: ServiceId): Boolean {
        if (serviceId.type != ServiceType.NODE) return false
        return exists(serviceId.id.toString())
    }

    suspend fun updateNode(cloudNode: CloudNode): CloudNode
        = updateService(cloudNode) as CloudNode

    suspend fun createNode(cloudNode: CloudNode): CloudNode
        = createService(cloudNode) as CloudNode


    suspend fun getMasterNode(): CloudNode?
        = getConnectedNodes().firstOrNull { it.master }

    suspend fun getConnectedNodes(): List<CloudNode> =
        getConnectedServices().filter { it.serviceId.type == ServiceType.NODE } as List<CloudNode>

}