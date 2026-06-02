package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository

class CloudServerVersionTypeParser(
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository
) : ICommandArgumentParser<CloudServerVersionType> {

    override suspend fun parse(parameter: String): CloudServerVersionType? {
        return serverVersionTypeRepository.getTypes().firstOrNull { it.name.equals(parameter, ignoreCase = true) }
    }
}
