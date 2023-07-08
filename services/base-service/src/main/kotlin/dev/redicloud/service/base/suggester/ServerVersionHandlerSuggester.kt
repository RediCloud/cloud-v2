package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.AbstractCommandSuggester
import dev.redicloud.repository.server.version.handler.IServerVersionHandler

class ServerVersionHandlerSuggester : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        IServerVersionHandler.CACHE_HANDLERS.map { it.name }.toTypedArray()


}