package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import java.util.*

class CloudServerVersionParser(private val serverVersionRepository: CloudServerVersionRepository) :
    ICommandArgumentParser<CloudServerVersion> {

    override suspend fun parse(parameter: String): CloudServerVersion? {
        return try {
            val uniqueId = UUID.fromString(parameter)
            serverVersionRepository.getVersion(uniqueId)
        } catch (_: IllegalArgumentException) {
            serverVersionRepository.getVersion(parameter)
        }
    }
}
