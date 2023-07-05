package dev.redicloud.service.base.suggester

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.utils.SingleCache
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class RegisteredCloudNodeSuggester(private val nodeRepository: NodeRepository) : ICommandSuggester {

    override fun suggest(context: CommandContext): Array<String> =
        runBlocking { nodeRepository.getRegisteredNodes().map { it.name }.toTypedArray() }

}