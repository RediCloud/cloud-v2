package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.isServiceId
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.node.NodeRepository

class CloudNodeParser(private val nodeRepository: NodeRepository) : ICommandArgumentParser<CloudNode> {

    override suspend fun parse(parameter: String): CloudNode? {
        try {
            if (parameter.isServiceId()) {
                val serviceId = ServiceId.fromString(parameter)
                return nodeRepository.getNode(serviceId)
            }
            return nodeRepository.getRegisteredNodes()
                .firstOrNull { it.name.equals(parameter, ignoreCase = true) }
        } catch (_: Exception) {
            return null
        }
    }
}
