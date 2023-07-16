package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import kotlinx.coroutines.runBlocking

class CloudServerVersionSuggester(
    private val cloudServerVersionRepository: CloudServerVersionRepository
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { cloudServerVersionRepository.getVersions().map { it.getDisplayName() }.toTypedArray() }
}