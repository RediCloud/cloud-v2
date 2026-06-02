package dev.redicloud.service.base.suggester

import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.commands.CommandContext
import dev.redicloud.repository.node.NodeRepository

class RegisteredCloudNodeSuggester(private val nodeRepository: NodeRepository) : AbstractCommandSuggester() {

    override suspend fun suggest(context: CommandContext): Array<String> =
        nodeRepository.getRegisteredNodes().map { it.name }.toTypedArray()
}
