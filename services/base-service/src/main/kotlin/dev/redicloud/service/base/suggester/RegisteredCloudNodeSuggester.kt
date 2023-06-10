package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandSuggesterContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.node.NodeRepository
import kotlinx.coroutines.runBlocking

class RegisteredCloudNodeSuggester(private val nodeRepository: NodeRepository) : ICommandSuggester {

    override fun suggest(context: CommandSuggesterContext): Array<String> {
        return runBlocking { nodeRepository.getRegisteredNodes().map { it.name }.toTypedArray() }
    }

}