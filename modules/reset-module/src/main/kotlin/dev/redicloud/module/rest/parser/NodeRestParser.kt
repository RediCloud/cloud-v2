package dev.redicloud.module.rest.parser

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.service.node.ICloudNode
import dev.redicloud.api.service.node.ICloudNodeRepository
import java.util.*

class NodeRestParser(
    private val nodeRepository: ICloudNodeRepository
) {

    suspend fun parseIdToNode(handler: () -> String?): ICloudNode? {
        val id = handler() ?: return null
        return try {
            val uuid = UUID.fromString(id)
            nodeRepository.getNode(ServiceId(uuid, ServiceType.NODE))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

}