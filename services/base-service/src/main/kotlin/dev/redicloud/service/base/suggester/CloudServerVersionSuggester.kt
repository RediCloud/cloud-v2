package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.AbstractCommandSuggester
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import kotlinx.coroutines.runBlocking

class CloudServerVersionSuggester(
    private val cloudServerVersionRepository: CloudServerVersionRepository
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { cloudServerVersionRepository.getVersions().map { it.getDisplayName() }.toTypedArray() }
}