package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.repository.java.version.JavaVersionRepository

class JavaVersionSuggester(
    private val javaVersionRepository: JavaVersionRepository
) : AbstractCommandSuggester() {

    override suspend fun suggest(context: CommandContext): Array<String> =
        javaVersionRepository.getVersions().map { it.name }.toTypedArray()
}
