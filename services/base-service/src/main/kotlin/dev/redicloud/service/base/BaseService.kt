package dev.redicloud.service.base

import dev.redicloud.cache.tasks.InvalidCacheTask
import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.api.packets.PacketListener
import dev.redicloud.commands.api.PARSERS
import dev.redicloud.commands.api.SUGGESTERS
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.java.version.JavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.player.PlayerRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.service.base.packets.*
import dev.redicloud.service.base.packets.listener.CloudServiceShutdownPacketListener
import dev.redicloud.service.base.parser.*
import dev.redicloud.service.base.suggester.*
import dev.redicloud.service.base.utils.ClusterConfiguration
import dev.redicloud.tasks.CloudTaskManager
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.ioScope
import dev.redicloud.utils.loadProperties
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.logging.Level
import kotlin.reflect.KClass
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

abstract class BaseService(
    databaseConfiguration: DatabaseConfiguration,
    _databaseConnection: DatabaseConnection?,
    val serviceId: ServiceId
) {

    companion object {
        val LOGGER = LogManager.logger(BaseService::class)
        var SHUTTINGDOWN = false
    }

    val databaseConnection: DatabaseConnection

    val nodeRepository: NodeRepository
    val serverVersionRepository: CloudServerVersionRepository
    abstract val fileTemplateRepository: AbstractFileTemplateRepository
    abstract val serverVersionTypeRepository: CloudServerVersionTypeRepository
    val serverRepository: ServerRepository
    val configurationTemplateRepository: ConfigurationTemplateRepository
    val javaVersionRepository: JavaVersionRepository
    val playerRepository: PlayerRepository

    val packetManager: PacketManager
    val eventManager: EventManager
    val taskManager: CloudTaskManager
    val clusterConfiguration: ClusterConfiguration

    init {
        runBlocking {
            loadProperties(Thread.currentThread().contextClassLoader)
            ServerVersion.loadIfNotLoaded()
        }
        databaseConnection = if (_databaseConnection != null && _databaseConnection.isConnected()) {
            _databaseConnection
        } else {
            DatabaseConnection(
                databaseConfiguration,
                serviceId,
                GsonCodec()
            )
        }
        try {
            if (!databaseConnection.isConnected()) runBlocking { databaseConnection.connect() }
        } catch (e: Exception) {
            LOGGER.severe("Failed to connect to database", e)
            exitProcess(-1)
        }

        clusterConfiguration = ClusterConfiguration(databaseConnection)

        packetManager = PacketManager(databaseConnection, serviceId)
        eventManager = EventManager("base-event-manager", packetManager)
        val taskThreads = when(serviceId.type) {
            ServiceType.NODE -> 4
            ServiceType.MINECRAFT_SERVER -> 1
            ServiceType.PROXY_SERVER -> 2
            else -> 2
        }
        taskManager = CloudTaskManager(eventManager, packetManager, taskThreads)
        taskManager.builder()
            .task(InvalidCacheTask())
            .period(30.seconds)
            .register()

        playerRepository = PlayerRepository(databaseConnection, eventManager, packetManager)
        javaVersionRepository = JavaVersionRepository(serviceId, databaseConnection, packetManager)
        nodeRepository = NodeRepository(databaseConnection, serviceId, packetManager, eventManager)
        serverVersionRepository = CloudServerVersionRepository(databaseConnection, packetManager)
        configurationTemplateRepository = ConfigurationTemplateRepository(databaseConnection, eventManager, packetManager)
        serverRepository = ServerRepository(databaseConnection, serviceId, packetManager, eventManager, configurationTemplateRepository)
        this.registerPackets()
        this.registerPacketListeners()
    }

    fun registerDefaults() {
        this.registerDefaultParsers()
        this.registerDefaultSuggesters()
    }

    open fun shutdown(force: Boolean = false) {
        SHUTTINGDOWN = true
        runBlocking {
            nodeRepository.shutdownAction.run()
            serverRepository.shutdownAction.run()
            taskManager.getTasks().forEach { it.cancel() }
            packetManager.disconnect()
            databaseConnection.disconnect()
            defaultScope.cancel()
            ioScope.cancel()
        }
    }

    fun sendClusterMessage(message: String, level: Level = Level.INFO, vararg serviceIds: ServiceId) {
        ioScope.launch {
            packetManager.publish(ClusterMessagePacket(message, level), *serviceIds)
        }
    }

    fun sendClusterMessage(message: String, level: Level = Level.INFO, serviceTargetType: ServiceType) {
        ioScope.launch {
            packetManager.publish(ClusterMessagePacket(message, level), serviceTargetType)
        }
    }

    private fun registerDefaultParsers() {
        PARSERS[CloudNode::class] = CloudNodeParser(this.nodeRepository)
        PARSERS[CloudServer::class] = CloudServerParser(this.serverRepository)
        PARSERS[CloudServerVersion::class] = CloudServerVersionParser(this.serverVersionRepository)
        PARSERS[CloudServerVersionType::class] = CloudServerVersionTypeParser(this.serverVersionTypeRepository)
        PARSERS[JavaVersion::class] = JavaVersionParser(this.javaVersionRepository)
        PARSERS[ServerVersion::class] = ServerVersionParser()
        PARSERS[ConfigurationTemplate::class] = ConfigurationTemplateParser(this.configurationTemplateRepository)
        PARSERS[IServerVersionHandler::class] = ServerVersionHandlerParser()
        PARSERS[FileTemplate::class] = FileTemplateParser(this.fileTemplateRepository)
    }

    private fun registerDefaultSuggesters() {
        SUGGESTERS.add(RegisteredCloudNodeSuggester(this.nodeRepository))
        SUGGESTERS.add(ConnectedCloudNodeSuggester(this.nodeRepository))
        SUGGESTERS.add(CloudServerVersionSuggester(this.serverVersionRepository))
        SUGGESTERS.add(CloudServerVersionTypeSuggester(this.serverVersionTypeRepository))
        SUGGESTERS.add(ConfigurationTemplateSuggester(this.configurationTemplateRepository))
        SUGGESTERS.add(JavaVersionSuggester(this.javaVersionRepository))
        SUGGESTERS.add(ServerVersionSuggester())
        SUGGESTERS.add(ServerVersionHandlerSuggester())
        SUGGESTERS.add(FileTemplateSuggester(this.fileTemplateRepository))
        SUGGESTERS.add(CloudServerSuggester(this.serverRepository))
        SUGGESTERS.add(CloudConnectorFileNameSelector())
    }

    private fun registerPackets() {
        packetManager.registerPacket(ServicePingPacket::class)
        packetManager.registerPacket(ServicePingResponse::class)
        packetManager.registerPacket(CloudServiceShutdownPacket::class)
        packetManager.registerPacket(CloudServiceShutdownResponse::class)
        packetManager.registerPacket(ClusterMessagePacket::class)
        packetManager.registerPacket(ScreenCommandPacket::class)
    }

    private fun registerPacketListeners() {
        fun register(listener: PacketListener<*>) {
            packetManager.registerListener(listener)
        }
        register(CloudServiceShutdownPacketListener(this))
    }

}