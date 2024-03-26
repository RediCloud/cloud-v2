package dev.redicloud.module.rest

import com.google.inject.name.Named
import dev.redicloud.api.commands.ICommandManager
import dev.redicloud.api.modules.CloudModule
import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.api.modules.ModuleTask
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplateRepository
import dev.redicloud.api.template.file.ICloudFileTemplateRepository
import dev.redicloud.api.utils.CloudInjectable
import dev.redicloud.api.version.ICloudServerVersionRepository
import dev.redicloud.api.version.ICloudServerVersionTypeRepository
import dev.redicloud.logging.LogManager
import dev.redicloud.module.rest.fetcher.*
import dev.redicloud.module.rest.handler.node.NodeInfoHandler
import dev.redicloud.module.rest.handler.player.PlayerInfoHandler
import dev.redicloud.module.rest.handler.server.MinecraftServerInfoHandler
import dev.redicloud.module.rest.handler.server.ProxyServerInfoHandler
import dev.redicloud.module.rest.handler.version.ServerVersionInfoHandler
import dev.redicloud.module.rest.handler.version.type.ServerVersionTypeInfoHandler
import io.javalin.Javalin
import kotlinx.coroutines.runBlocking

class RestModule : CloudModule(), CloudInjectable {

    companion object {
        private val logger = LogManager.logger(RestModule::class)
    }

    lateinit var app: Javalin
    val port: Int = System.getProperty("redicloud.rest.port", "8787").toIntOrNull() ?: 8787

    lateinit var config: IModuleStorage

    lateinit var playerFetcher: PlayerFetcher
    lateinit var nodeFetcher: NodeFetcher
    lateinit var serverFetcher: ServerFetcher
    lateinit var serverVersionFetcher: ServerVersionFetcher
    lateinit var serverVersionTypeFetcher: ServerVersionTypeFetcher
    lateinit var fileTemplateFetcher: FileTemplateFetcher
    lateinit var configurationTemplateFetcher: ConfigurationTemplateFetcher

    @ModuleTask(ModuleLifeCycle.LOAD)
    fun load(
        @Named("this") nodeId: ServiceId,
        nodeRepository: ICloudNodeRepository,
        serverRepository: ICloudServerRepository,
        playerRepository: ICloudPlayerRepository,
        serverVersionRepository: ICloudServerVersionRepository,
        serverVersionTypeRepository: ICloudServerVersionTypeRepository,
        fileTemplateRepository: ICloudFileTemplateRepository,
        configurationTemplateRepository: ICloudConfigurationTemplateRepository
    ) {
        logger.info("Starting rest module on port $port...")
        app = Javalin.create()
        config = getStorage("rest-server")
        val node = runBlocking { nodeRepository.getNode(nodeId)!! }

        playerFetcher = PlayerFetcher(playerRepository)
        nodeFetcher = NodeFetcher(nodeRepository)
        serverFetcher = ServerFetcher(serverRepository)
        serverVersionFetcher = ServerVersionFetcher(serverVersionRepository)
        serverVersionTypeFetcher = ServerVersionTypeFetcher(serverVersionTypeRepository)
        fileTemplateFetcher = FileTemplateFetcher(fileTemplateRepository)
        configurationTemplateFetcher = ConfigurationTemplateFetcher(configurationTemplateRepository)

        register(NodeInfoHandler(nodeRepository, nodeFetcher, config))
        register(PlayerInfoHandler(playerRepository, playerFetcher, config))
        register(MinecraftServerInfoHandler(serverRepository, serverFetcher, config))
        register(ProxyServerInfoHandler(serverRepository, serverFetcher, config))
        register(ServerVersionInfoHandler(serverVersionRepository, serverVersionFetcher, config))
        register(ServerVersionTypeInfoHandler(serverVersionTypeRepository, serverVersionTypeFetcher, config))

        app.start(port)
    }

    private fun register(handler: RestHandler) {
        app.get(handler.path, handler)
    }

}