package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.utils.EasyCache
import dev.redicloud.utils.SingleCache
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class CloudServerSuggester(
    private val serverRepository: ServerRepository
) : ICommandSuggester{

    private val easyCache = SingleCache(5.seconds) { serverRepository.getRegisteredServers().map { it.name }.toTypedArray() }

    override fun suggest(context: CommandContext): Array<String> =
        easyCache.get()!!

}