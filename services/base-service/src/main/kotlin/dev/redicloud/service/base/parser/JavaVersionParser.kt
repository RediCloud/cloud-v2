package dev.redicloud.service.base.parser

import dev.redicloud.commands.api.CommandArgumentParser
import dev.redicloud.repository.java.version.JavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import kotlinx.coroutines.runBlocking

class JavaVersionParser(
    private val javaVersionRepository: JavaVersionRepository
) : CommandArgumentParser<JavaVersion> {

    override fun parse(parameter: String): JavaVersion? {
        return runBlocking {
            javaVersionRepository.getVersions().firstOrNull { it.name.lowercase() == parameter.lowercase() }
        }
    }


}