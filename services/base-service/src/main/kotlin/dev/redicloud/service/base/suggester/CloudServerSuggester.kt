package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.AbstractCommandSuggester
import dev.redicloud.repository.server.ServerRepository
import kotlinx.coroutines.runBlocking

class CloudServerSuggester(
    private val serverRepository: ServerRepository
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { serverRepository.getRegisteredServers().map { it.name }.toTypedArray() }

}