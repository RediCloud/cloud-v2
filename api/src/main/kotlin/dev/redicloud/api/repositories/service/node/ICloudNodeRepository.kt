package dev.redicloud.api.repositories.service.node

import dev.redicloud.utils.service.ServiceId

interface ICloudNodeRepository {

    suspend fun getNode(serviceId: ServiceId): ICloudNode?

    suspend fun existsNode(serviceId: ServiceId): Boolean

    suspend fun updateNode(cloudNode: ICloudNode): ICloudNode

    suspend fun getMasterNode(): ICloudNode?

    suspend fun getConnectedNodes(): List<ICloudNode>

    suspend fun getRegisteredNodes(): List<ICloudNode>

}