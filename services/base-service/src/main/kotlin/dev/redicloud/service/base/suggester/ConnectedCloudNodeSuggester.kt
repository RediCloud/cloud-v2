package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.node.NodeRepository
import kotlinx.coroutines.runBlocking

class ConnectedCloudNodeSuggester(private val nodeRepository: NodeRepository) : ICommandSuggester {

    override fun suggest(context: CommandContext): Array<String> {
        return runBlocking { nodeRepository.getConnectedNodes().map { it.name }.toTypedArray() }
    }

}