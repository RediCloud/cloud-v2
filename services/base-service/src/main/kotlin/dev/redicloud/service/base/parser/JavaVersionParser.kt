package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.java.version.CloudJavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository

class JavaVersionParser(
    private val javaVersionRepository: JavaVersionRepository
) : ICommandArgumentParser<CloudJavaVersion> {

    override suspend fun parse(parameter: String): CloudJavaVersion? {
        return javaVersionRepository.getVersions().firstOrNull { it.name.equals(parameter, ignoreCase = true) }
    }
}
