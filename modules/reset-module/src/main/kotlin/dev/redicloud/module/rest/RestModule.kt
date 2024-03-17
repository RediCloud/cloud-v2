package dev.redicloud.module.rest

import dev.redicloud.api.modules.ICloudModule
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.api.modules.ModuleTask
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.api.utils.CloudInjectable
import dev.redicloud.logging.LogManager
import dev.redicloud.module.rest.handler.node.NodeConnectedHandler
import dev.redicloud.module.rest.handler.node.NodeInfoHandler
import dev.redicloud.module.rest.handler.node.NodeRegisteredHandler
import dev.redicloud.module.rest.handler.player.PlayerInfoHandler
import dev.redicloud.module.rest.handler.server.ServerConnectedHandler
import dev.redicloud.module.rest.handler.server.ServerRegisteredHandler
import dev.redicloud.module.rest.parser.NodeRestParser
import dev.redicloud.module.rest.parser.PlayerRestParser
import dev.redicloud.module.rest.parser.ServerRestParser
import io.javalin.Javalin
import org.redisson.api.RedissonClient

class RestModule : ICloudModule, CloudInjectable {

    companion object {
        private val logger = LogManager.logger(RestModule::class)
    }

    lateinit var app: Javalin
    val port: Int = System.getProperty("redicloud.rest.port", "8787").toIntOrNull() ?: 8787

    lateinit var config: RestConfiguration

    lateinit var playerParser: PlayerRestParser
    lateinit var nodeParser: NodeRestParser
    lateinit var serverParser: ServerRestParser

    @ModuleTask(ModuleLifeCycle.LOAD)
    fun load(
        nodeRepository: ICloudNodeRepository,
        serverRepository: ICloudServerRepository,
        playerRepository: ICloudPlayerRepository,
        redissonClient: RedissonClient
    ) {
        logger.info("Starting rest module on port $port...")
        app = Javalin.create()
        config = RestConfiguration(redissonClient)

        playerParser = PlayerRestParser(playerRepository)
        nodeParser = NodeRestParser(nodeRepository)
        serverParser = ServerRestParser(serverRepository)

        register(NodeRegisteredHandler(config, nodeRepository))
        register(NodeConnectedHandler(config, nodeRepository))
        register(NodeInfoHandler(config, nodeParser))

        register(PlayerInfoHandler(config, playerParser))

        register(ServerConnectedHandler(config, serverRepository))
        register(ServerRegisteredHandler(config, serverRepository))

        app.start(port)
    }

    fun register(handler: RestHandler) {
        app.get(handler.path, handler)
    }

}