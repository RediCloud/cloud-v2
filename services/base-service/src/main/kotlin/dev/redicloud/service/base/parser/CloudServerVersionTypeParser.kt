package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import kotlinx.coroutines.runBlocking

class CloudServerVersionTypeParser(
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository
) : ICommandArgumentParser<CloudServerVersionType> {

    override fun parse(parameter: String): CloudServerVersionType? {
        return runBlocking {
            serverVersionTypeRepository.getTypes().firstOrNull { it.name.lowercase() == parameter.lowercase() }
        }
    }


}