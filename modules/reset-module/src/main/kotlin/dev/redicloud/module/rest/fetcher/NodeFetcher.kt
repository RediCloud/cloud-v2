package dev.redicloud.module.rest.fetcher

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.service.node.ICloudNode
import dev.redicloud.api.service.node.ICloudNodeRepository
import java.util.*

class NodeFetcher(
    private val nodeRepository: ICloudNodeRepository
) {

    suspend fun fetchNodeById(id: String?): ICloudNode? {
        if (id == null) {
            return null
        }
        return try {
            val uuid = UUID.fromString(id)
            nodeRepository.getNode(ServiceId(uuid, ServiceType.NODE))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

}