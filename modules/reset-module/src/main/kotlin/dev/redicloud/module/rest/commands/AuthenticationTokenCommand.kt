package dev.redicloud.module.rest.commands

import dev.redicloud.api.commands.*
import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.api.modules.getList
import dev.redicloud.api.modules.getListOrDefault
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.utils.toBase64
import kotlinx.coroutines.runBlocking
import java.util.UUID

@Command("auth-token")
class AuthenticationTokenCommand(
    private val nodeId: UUID,
    private val hostname: String,
    private val port: Int,
    private val config: IModuleStorage,
    private val nodeRepository: ICloudNodeRepository
) : ICommand {

    @CommandSubPath("generate")
    @CommandDescription("Creates a new authentication token for the current node")
    fun create(
        actor: ICommandActor<*>
    ) = runBlocking {
        val accessToken = (1..27).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
        val token = "$hostname|$port|$accessToken".toBase64()
        val tokens = config.getListOrDefault<String>("auth-tokens-$nodeId") { emptyList() }.toMutableList()
        tokens.add(token)
        config.set("auth-tokens-$nodeId", tokens)
        actor.sendMessage("Created new cluster authentication token: %hc%$token")
        actor.sendMessage("§cPlease note that this token is only valid for this node and should be kept secret!")
    }

    @CommandSubPath("delete")
    @CommandDescription("Deletes an authentication token")
    fun delete(
        actor: ICommandActor<*>,
        @CommandParameter("token") token: String
    ) = runBlocking {
        config.keys().filter { it.startsWith("auth-tokens-") }.forEach {  key ->
            val tokens = config.getList<String>(key).toMutableList()
            if (tokens.contains(token)) {
                tokens.remove(token)
                config.set(key, tokens)
                actor.sendMessage("Deleted cluster authentication token: %hc%$token")
                return@runBlocking
            }
        }
    }

    @CommandSubPath("list")
    @CommandDescription("Lists all authentication tokens")
    fun list(
        actor: ICommandActor<*>
    ) = runBlocking {
        config.keys().filter { it.startsWith("auth-tokens-") }.forEach { key ->
            val tokens = config.getList<String>(key)
            val node = nodeRepository.getNode(ServiceId(UUID.fromString(key.removePrefix("auth-tokens-")), ServiceType.NODE))
            actor.sendMessage("Tokens for %hc%${node?.name ?: "unknown-node"}%: %hc%${tokens.joinToString(", ")}")
        }
    }

}