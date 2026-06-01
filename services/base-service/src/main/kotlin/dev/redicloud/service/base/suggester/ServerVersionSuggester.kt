package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.repository.server.version.serverversion.VersionRepository

class ServerVersionSuggester : AbstractCommandSuggester() {

    override suspend fun suggest(context: CommandContext): Array<String> {
        VersionRepository.loadIfNotLoaded()
        return VersionRepository.versions().map { it.name }.toTypedArray()
    }
}
