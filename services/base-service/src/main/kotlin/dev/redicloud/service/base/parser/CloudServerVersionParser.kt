package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import kotlinx.coroutines.runBlocking
import java.util.*

class CloudServerVersionParser(private val serverVersionRepository: CloudServerVersionRepository) :
    CommandArgumentParser<CloudServerVersion> {

    override fun parse(parameter: String): CloudServerVersion? {
        return runBlocking {
            try {
                val uniqueId = UUID.fromString(parameter)
                serverVersionRepository.getVersion(uniqueId)
            } catch (e: IllegalArgumentException) {
                serverVersionRepository.getVersion(parameter)
            }
        }
    }
}