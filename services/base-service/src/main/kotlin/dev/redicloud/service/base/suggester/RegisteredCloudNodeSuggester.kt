package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.repository.node.NodeRepository
import kotlinx.coroutines.runBlocking

class RegisteredCloudNodeSuggester(private val nodeRepository: NodeRepository) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { nodeRepository.getRegisteredNodes().map { it.name }.toTypedArray() }

}