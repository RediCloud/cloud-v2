package dev.redicloud.service.base.parser

import dev.redicloud.commands.api.CommandArgumentParser
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import kotlinx.coroutines.runBlocking

class CloudServerVersionTypeParser(
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository
) : CommandArgumentParser<CloudServerVersionType> {

    override fun parse(parameter: String): CloudServerVersionType? {
        return runBlocking {
            serverVersionTypeRepository.getTypes().firstOrNull { it.name.lowercase() == parameter.lowercase() }
        }
    }


}