package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.isServiceId
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository

class CloudServerParser(private val serverRepository: ServerRepository) : ICommandArgumentParser<CloudServer> {

    override suspend fun parse(parameter: String): CloudServer? {
        try {
            if (parameter.isServiceId()) {
                val serviceId = ServiceId.fromString(parameter)
                return serverRepository.getServer(serviceId)
            }
            return serverRepository.getRegisteredServers()
                .firstOrNull { it.name.equals(parameter, ignoreCase = true) }
        } catch (_: Exception) {
            return null
        }
    }
}
