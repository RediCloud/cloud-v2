package dev.redicloud.cluster.file

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.service.ServiceRepository
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType

class FileNodeRepository(databaseConnection: DatabaseConnection, packetManager: PacketManager) :
    ServiceRepository<FileNode>(databaseConnection, databaseConnection.serviceId, ServiceType.FILE_NODE, packetManager) {

     fun migrateId(serviceId: ServiceId): ServiceId {
        return when (serviceId.type) {
            ServiceType.NODE -> ServiceId(serviceId.id, ServiceType.FILE_NODE)
            ServiceType.FILE_NODE -> serviceId
            else -> throw IllegalArgumentException("Cannot migrate service id $serviceId to file node id")
        }
    }

    suspend fun getFileNode(serviceId: ServiceId): FileNode? {
        return getService(migrateId(serviceId)) as FileNode?
    }

    suspend fun existsFileNode(serviceId: ServiceId): Boolean {
        return existsService(migrateId(serviceId))
    }

    suspend fun updateFileNode(fileNode: FileNode): FileNode {
        return updateService(fileNode) as FileNode
    }

    suspend fun createFileNode(fileNode: FileNode): FileNode {
        return createService(fileNode) as FileNode
    }

}