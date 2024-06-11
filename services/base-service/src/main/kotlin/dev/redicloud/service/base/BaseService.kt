package dev.redicloud.service.base

import com.google.inject.Guice
import com.google.inject.name.Names
import dev.redicloud.api.database.IDatabaseConnection
import dev.redicloud.api.events.IEventManager
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.cache.tasks.InvalidCacheTask
import dev.redicloud.api.packets.PacketListener
import dev.redicloud.api.java.ICloudJavaVersionRepository
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplateRepository
import dev.redicloud.api.version.ICloudServerVersionRepository
import dev.redicloud.api.version.ICloudServerVersionTypeRepository
import dev.redicloud.commands.api.PARSERS
import dev.redicloud.commands.api.SUGGESTERS
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.java.version.CloudJavaVersion
import dev.redicloud.repository.java.version.JavaVersionRepository
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.player.PlayerRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionType
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.api.version.IServerVersionHandler
import dev.redicloud.logging.Logger
import dev.redicloud.repository.server.version.serverversion.ServerVersion
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
import dev.redicloud.utils.InjectorModule
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.ioScope
import dev.redicloud.utils.loadProperties
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.template.file.ICloudFileTemplateRepository
import dev.redicloud.api.utils.injector
import dev.redicloud.api.version.IVersionRepository
import dev.redicloud.console.Console
import dev.redicloud.modules.ModuleHandler
import dev.redicloud.repository.server.version.serverversion.VersionRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.redisson.api.RedissonClient
import java.util.logging.Level
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

abstract class BaseService(
    databaseConfiguration: DatabaseConfiguration,
    _databaseConnection: DatabaseConnection?,
    val serviceId: ServiceId
) : InjectorModule() {

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
    abstract val moduleHandler: ModuleHandler

    init {
        runBlocking {
            loadProperties(Thread.currentThread().contextClassLoader)
            VersionRepository.loadIfNotLoaded()
        }
        databaseConnection = if (_databaseConnection != null && _databaseConnection.connected) {
            _databaseConnection
        } else {
            DatabaseConnection(
                databaseConfiguration,
                serviceId
            )
        }
        try {
            if (!databaseConnection.connected) runBlocking { databaseConnection.connect() }
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
        nodeRepository = NodeRepository(databaseConnection, packetManager, eventManager)
        serverVersionRepository = CloudServerVersionRepository(databaseConnection, packetManager)
        configurationTemplateRepository = ConfigurationTemplateRepository(databaseConnection, eventManager, packetManager)
        serverRepository = ServerRepository(databaseConnection, serviceId, packetManager, eventManager)
        this.registerPackets()
        this.registerPacketListeners()
    }

    protected fun loadModuleRepositoryUrls(): List<String> {
        val moduleRepositoryUrls = clusterConfiguration.getList<String>("module-repositories", emptyList()).toMutableList()
        val defaultRepoUrl = System.getProperty("redicloud.modules.default.repo", "https://api.redicloud.dev/module-repository")
        if (!moduleRepositoryUrls.contains(defaultRepoUrl)) {
            moduleRepositoryUrls.add(defaultRepoUrl)
            clusterConfiguration.set("module-repositories", moduleRepositoryUrls)
        }
        return moduleRepositoryUrls
    }

    protected fun initApi() {
        if (serviceId.type.isServer()) LOGGER.info("Initializing RediCloud API!")
        injector = Guice.createInjector(this)
    }

    fun registerDefaults() {
        this.registerDefaultParsers()
        this.registerDefaultSuggesters()
    }

    open fun plattformShutdown(){}

    open fun shutdown(force: Boolean = false) {
        SHUTTINGDOWN = true
        runBlocking {
            moduleHandler.unloadModules()
            nodeRepository.shutdownAction.run()
            serverRepository.shutdownAction.run()
            taskManager.getTasks().forEach { it.cancel() }
            packetManager.disconnect()
            databaseConnection.disconnect()
            defaultScope.cancel()
            ioScope.cancel()
            Console.Companion.CURRENT_CONSOLE?.close(true)
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
        PARSERS[CloudJavaVersion::class] = JavaVersionParser(this.javaVersionRepository)
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

    override fun configure() {
        bind(IPacketManager::class).toInstance(packetManager)
        bind(IEventManager::class).toInstance(eventManager)
        bind(ICloudPlayerRepository::class).toInstance(playerRepository)
        bind(ICloudNodeRepository::class).toInstance(nodeRepository)
        bind(ICloudServerVersionRepository::class).toInstance(serverVersionRepository)
        bind(ICloudServerVersionTypeRepository::class).toInstance(serverVersionTypeRepository)
        bind(ICloudServerRepository::class).toInstance(serverRepository)
        bind(ICloudConfigurationTemplateRepository::class).toInstance(configurationTemplateRepository)
        bind(ICloudJavaVersionRepository::class).toInstance(javaVersionRepository)
        bind(ICloudFileTemplateRepository::class).toInstance(fileTemplateRepository)
        bind(IVersionRepository::class).toInstance(VersionRepository)
        bind(Logger::class).annotatedWith(Names.named("base")).toInstance(LOGGER)
        bind(ServiceId::class).annotatedWith(Names.named("this")).toInstance(serviceId)
        bind(java.util.logging.Logger::class).annotatedWith(Names.named("root")).toInstance(LogManager.rootLogger())
        bind(java.util.logging.Logger::class).annotatedWith(Names.named("service")).toInstance(LOGGER)
        bind(CloudTaskManager::class).toInstance(taskManager)
        if (System.getProperty("redicloud.inject.redisson", "true").toBooleanStrictOrNull() == true) {
            bind(RedissonClient::class).toInstance(databaseConnection.client)
        }
        bind(ClusterConfiguration::class).toInstance(clusterConfiguration)
        bind(IDatabaseConnection::class).toInstance(databaseConnection)
    }

}