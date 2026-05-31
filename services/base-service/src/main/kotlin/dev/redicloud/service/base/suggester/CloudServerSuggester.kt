package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.repository.server.ServerRepository

class CloudServerSuggester(
    private val serverRepository: ServerRepository
) : AbstractCommandSuggester() {

    override suspend fun suggest(context: CommandContext): Array<String> =
        serverRepository.getRegisteredServers().map { it.name }.toTypedArray()
}
