package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.isServiceId
import kotlinx.coroutines.runBlocking

class CloudServerParser(private val serverRepository: ServerRepository) : ICommandArgumentParser<CloudServer> {

    override fun parse(parameter: String): CloudServer? {
        return runBlocking {
            try {
                if (parameter.isServiceId()) {
                    val serviceId = ServiceId.fromString(parameter)
                    return@runBlocking serverRepository.getServer(serviceId)
                }
                return@runBlocking serverRepository.getRegisteredServers()
                    .firstOrNull() { it.name.lowercase() == parameter.lowercase() }
            }catch (e: Exception) {
                return@runBlocking null
            }
        }
    }

}