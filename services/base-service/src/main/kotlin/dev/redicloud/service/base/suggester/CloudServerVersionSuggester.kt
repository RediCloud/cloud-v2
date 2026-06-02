package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.repository.server.version.CloudServerVersionRepository

class CloudServerVersionSuggester(
    private val cloudServerVersionRepository: CloudServerVersionRepository
) : AbstractCommandSuggester() {

    override suspend fun suggest(context: CommandContext): Array<String> =
        cloudServerVersionRepository.getVersions().map { it.displayName }.toTypedArray()
}
