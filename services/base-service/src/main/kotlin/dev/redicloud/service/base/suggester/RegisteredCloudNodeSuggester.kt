package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.AbstractCommandSuggester
import dev.redicloud.repository.node.NodeRepository
import kotlinx.coroutines.runBlocking

class RegisteredCloudNodeSuggester(private val nodeRepository: NodeRepository) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { nodeRepository.getRegisteredNodes().map { it.name }.toTypedArray() }

}