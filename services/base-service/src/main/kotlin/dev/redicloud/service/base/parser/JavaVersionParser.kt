package dev.redicloud.service.base.parser

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.repository.java.version.CloudJavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import kotlinx.coroutines.runBlocking

class JavaVersionParser(
    private val javaVersionRepository: JavaVersionRepository
) : ICommandArgumentParser<CloudJavaVersion> {

    override fun parse(parameter: String): CloudJavaVersion? {
        return runBlocking {
            javaVersionRepository.getVersions().firstOrNull { it.name.lowercase() == parameter.lowercase() }
        }
    }


}