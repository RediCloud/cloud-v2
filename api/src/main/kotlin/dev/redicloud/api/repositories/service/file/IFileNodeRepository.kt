package dev.redicloud.api.repositories.service.file

import dev.redicloud.utils.service.ServiceId

interface IFileNodeRepository {

    suspend fun getFileNode(serviceId: ServiceId): IFileNode?

    suspend fun existsFileNode(serviceId: ServiceId): Boolean

    suspend fun updateFileNode(fileNode: IFileNode): IFileNode

    suspend fun createFileNode(fileNode: IFileNode): IFileNode

    suspend fun getRegisteredFileNodes(): List<IFileNode>

    suspend fun getConnectedFileNodes(): List<IFileNode>

}