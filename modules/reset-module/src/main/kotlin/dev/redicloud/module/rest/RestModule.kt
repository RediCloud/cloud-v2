package dev.redicloud.module.rest

import dev.redicloud.api.modules.ICloudModule
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.api.modules.ModuleTask
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.api.utils.CloudInjectable
import dev.redicloud.logging.LogManager
import dev.redicloud.module.rest.handler.node.NodeConnectedHandler
import dev.redicloud.module.rest.handler.node.NodeInfoHandler
import dev.redicloud.module.rest.handler.node.NodeRegisteredHandler
import dev.redicloud.module.rest.handler.server.ServerConnectedHandler
import dev.redicloud.module.rest.handler.server.ServerRegisteredHandler
import io.javalin.Javalin

class RestModule : ICloudModule, CloudInjectable {

    companion object {
        private val logger = LogManager.logger(RestModule::class)
    }

    lateinit var app: Javalin
    val port: Int = System.getProperty("redicloud.rest.port", "8787").toIntOrNull() ?: 8787

    @ModuleTask(ModuleLifeCycle.LOAD)
    fun load(
        nodeRepository: ICloudNodeRepository,
        serverRepository: ICloudServerRepository
    ) {
        logger.info("Starting rest module on port $port...")
        app = Javalin.create()

        register(NodeRegisteredHandler(nodeRepository))
        register(NodeConnectedHandler(nodeRepository))
        register(NodeInfoHandler(nodeRepository))

        register(ServerConnectedHandler(serverRepository))
        register(ServerRegisteredHandler(serverRepository))

        app.start(port)
    }

    fun register(handler: RestHandler) {
        app.get(handler.path, handler)
    }

}