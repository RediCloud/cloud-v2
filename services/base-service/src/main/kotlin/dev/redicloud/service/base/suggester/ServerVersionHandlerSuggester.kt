package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.utils.SingleCache
import kotlin.time.Duration.Companion.seconds

class ServerVersionHandlerSuggester : ICommandSuggester {

    override fun suggest(context: CommandContext): Array<String> =
        IServerVersionHandler.CACHE_HANDLERS.map { it.name }.toTypedArray()


}