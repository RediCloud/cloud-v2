package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.server.version.handler.IServerVersionHandler

class ServerVersionHandlerSuggester : ICommandSuggester {

    override fun suggest(context: CommandContext): Array<String> {
        return IServerVersionHandler.CACHE_HANDLERS.map { it.name }.toTypedArray()
    }


}