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

    private val easyCache = SingleCache(5.seconds) {
        runBlocking { javaVersionRepository.getVersions().map { it.name }.toTypedArray() }
    }

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking {
            javaVersionRepository.getVersions().map { it.name }.toTypedArray()
        }

}