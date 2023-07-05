package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.utils.SingleCache
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class JavaVersionSuggester(
    private val javaVersionRepository: JavaVersionRepository
) : ICommandSuggester {


    override fun suggest(context: CommandContext): Array<String> =
        runBlocking {
            javaVersionRepository.getVersions().map { it.name }.toTypedArray()
        }

}