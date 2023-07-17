package dev.redicloud.cluster.file

import dev.redicloud.api.repositories.service.file.IFileNode
import dev.redicloud.api.repositories.service.file.IFileNodeRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.service.CachedServiceRepository
import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlin.time.Duration.Companion.minutes

class FileNodeRepository(
    databaseConnection: DatabaseConnection,
    packetManager: PacketManager
) : ServiceRepository(
    databaseConnection,
    packetManager
), IFileNodeRepository {

    private val internalRepository = object : CachedServiceRepository<IFileNode, FileNode>(
        databaseConnection,
        ServiceType.FILE_NODE,
        packetManager,
        IFileNode::class,
        FileNode::class,
        5.minutes,
        this
    ) {
        override suspend fun transformShutdownable(service: FileNode): FileNode = service
    }

     fun migrateId(serviceId: ServiceId): ServiceId {
        return when (serviceId.type) {
            ServiceType.NODE -> ServiceId(serviceId.id, ServiceType.FILE_NODE)
            ServiceType.FILE_NODE -> serviceId
            else -> throw IllegalArgumentException("Cannot migrate service id $serviceId to file node id")
        }
    }

    override suspend fun getFileNode(serviceId: ServiceId): FileNode? {
        return internalRepository.getService(migrateId(serviceId))
    }

    override suspend fun existsFileNode(serviceId: ServiceId): Boolean {
        return internalRepository.existsService(migrateId(serviceId))
    }

    override suspend fun updateFileNode(fileNode: IFileNode): FileNode {
        return internalRepository.updateService(fileNode)
    }

    override suspend fun createFileNode(fileNode: IFileNode): FileNode {
        return internalRepository.createService(fileNode as FileNode)
    }

    override suspend fun getRegisteredFileNodes(): List<FileNode> {
        return internalRepository.getRegisteredServices()
    }

    override suspend fun getConnectedFileNodes(): List<FileNode> {
        return internalRepository.getConnectedServices()
    }

}