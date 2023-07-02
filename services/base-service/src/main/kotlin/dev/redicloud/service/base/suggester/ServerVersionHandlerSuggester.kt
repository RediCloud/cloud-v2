package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.utils.SingleCache
import kotlin.time.Duration.Companion.seconds

class ServerVersionHandlerSuggester : ICommandSuggester {

    private val easyCache = SingleCache(5.seconds) {
        IServerVersionHandler.CACHE_HANDLERS.map { it.name }.toTypedArray()
    }

    override fun suggest(context: CommandContext): Array<String> = easyCache.get()!!


}