package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.java.version.JavaVersionRepository
import kotlinx.coroutines.runBlocking

class JavaVersionSuggester(
    private val javaVersionRepository: JavaVersionRepository
) : ICommandSuggester {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking {
            javaVersionRepository.getVersions().map { it.name }.toTypedArray()
        }

}