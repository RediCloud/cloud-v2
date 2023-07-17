package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.isServiceId
import kotlinx.coroutines.runBlocking

class CloudNodeParser(private val nodeRepository: NodeRepository) : ICommandArgumentParser<CloudNode> {

    override fun parse(parameter: String): CloudNode? {
        return runBlocking {
            try {
                if (parameter.isServiceId()) {
                    val serviceId = ServiceId.fromString(parameter)
                    return@runBlocking nodeRepository.getNode(serviceId)
                }
                return@runBlocking nodeRepository.getRegisteredNodes()
                    .firstOrNull() { it.name.lowercase() == parameter.lowercase() }
            }catch (e: Exception) {
                return@runBlocking null
            }
        }
    }

}