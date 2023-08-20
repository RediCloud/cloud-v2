package dev.redicloud.api.service.node

import dev.redicloud.api.service.ServiceId

interface ICloudNodeRepository {

    suspend fun getNode(serviceId: ServiceId): ICloudNode?

    suspend fun existsNode(serviceId: ServiceId): Boolean

    suspend fun updateNode(cloudNode: ICloudNode): ICloudNode

    suspend fun getMasterNode(): ICloudNode?

    suspend fun getConnectedNodes(): List<ICloudNode>

    suspend fun getRegisteredNodes(): List<ICloudNode>

}