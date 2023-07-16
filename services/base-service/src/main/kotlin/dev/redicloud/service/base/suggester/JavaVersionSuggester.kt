package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.repository.java.version.JavaVersionRepository
import kotlinx.coroutines.runBlocking

class JavaVersionSuggester(
    private val javaVersionRepository: JavaVersionRepository
) : AbstractCommandSuggester() {


    override fun suggest(context: CommandContext): Array<String> =
        runBlocking {
            javaVersionRepository.getVersions().map { it.name }.toTypedArray()
        }

}