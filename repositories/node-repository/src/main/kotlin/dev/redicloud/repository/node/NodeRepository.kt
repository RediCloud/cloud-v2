package dev.redicloud.repository.node

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.event.EventManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.service.CachedServiceRepository
import dev.redicloud.api.events.impl.node.NodeDisconnectEvent
import dev.redicloud.api.service.node.ICloudNode
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import kotlin.time.Duration.Companion.minutes

class NodeRepository(
    databaseConnection: DatabaseConnection,
    packetManager: PacketManager,
    private val eventManager: EventManager,
) : ServiceRepository (
    databaseConnection,
    packetManager
), ICloudNodeRepository {

    val internalRepo = object : CachedServiceRepository<ICloudNode, CloudNode>(
        databaseConnection,
        ServiceType.NODE,
        packetManager,
        ICloudNode::class,
        CloudNode::class,
        5.minutes,
        this
    ) {
        override suspend fun transformShutdownable(service: CloudNode): CloudNode {
            service.currentMemoryUsage = 0
            service.hostedServers.clear()
            service.master = false
            eventManager.fireEvent(NodeDisconnectEvent(service.serviceId))
            return service
        }
    }.apply {
        internalRepositories.add(this)
    }

    override suspend fun getNode(serviceId: ServiceId): CloudNode? {
        return internalRepo.getService(serviceId)
    }

    override suspend fun existsNode(serviceId: ServiceId): Boolean {
        return internalRepo.existsService(serviceId)
    }

    override suspend fun updateNode(cloudNode: ICloudNode): CloudNode {
        return internalRepo.updateService(cloudNode)
    }

    suspend fun createNode(cloudNode: ICloudNode): CloudNode {
        return internalRepo.createService(cloudNode)
    }

    override suspend fun getMasterNode(): CloudNode? {
        return getConnectedNodes().firstOrNull { it.master }
    }

    override suspend fun getConnectedNodes(): List<CloudNode> {
        return internalRepo.getConnectedServices()
    }

    override suspend fun getRegisteredNodes(): List<CloudNode> {
        return internalRepo.getRegisteredServices()
    }

}