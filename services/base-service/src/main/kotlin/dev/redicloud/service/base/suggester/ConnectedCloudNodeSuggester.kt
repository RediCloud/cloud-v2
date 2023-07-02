package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.utils.SingleCache
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class ConnectedCloudNodeSuggester(private val nodeRepository: NodeRepository) : ICommandSuggester {

    private val easyCache = SingleCache(5.seconds) {
        runBlocking { nodeRepository.getConnectedNodes().map { it.name }.toTypedArray() }
    }
    override fun suggest(context: CommandContext): Array<String> = easyCache.get()!!

}