package dev.redicloud.service.node

import dev.redicloud.api.commands.ICommand
import dev.redicloud.api.commands.ICommandManager
import dev.redicloud.api.events.impl.server.CloudServerDisconnectedEvent
import dev.redicloud.cluster.file.FileCluster
import dev.redicloud.cluster.file.FileNodeRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.repository.java.version.CloudJavaVersion
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.api.version.IServerVersionHandler
import dev.redicloud.repository.server.version.task.CloudServerVersionUpdateTask
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.server.factory.task.*
import dev.redicloud.service.base.BaseService
import dev.redicloud.api.events.impl.node.NodeConnectEvent
import dev.redicloud.api.events.impl.node.NodeDisconnectEvent
import dev.redicloud.api.events.impl.node.NodeSuspendedEvent
import dev.redicloud.api.service.server.factory.ICloudRemoteServerFactory
import dev.redicloud.repository.server.version.handler.defaults.URLServerVersionHandler
import dev.redicloud.service.node.console.NodeConsole
import dev.redicloud.service.node.repository.node.connect
import dev.redicloud.service.node.commands.*
import dev.redicloud.service.node.repository.template.file.NodeFileTemplateRepository
import dev.redicloud.service.node.tasks.node.NodeChooseMasterTask
import dev.redicloud.service.node.tasks.NodePingTask
import dev.redicloud.service.node.tasks.NodeSelfSuspendTask
import dev.redicloud.service.node.tasks.metrics.MetricsTask
import dev.redicloud.api.utils.TEMP_FOLDER
import dev.redicloud.console.Console
import dev.redicloud.modules.ModuleHandler
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class NodeService(
    databaseConfiguration: DatabaseConfiguration,
    databaseConnection: DatabaseConnection,
    val configuration: NodeConfiguration,
    val firstStart: Boolean = false
) : BaseService(databaseConfiguration, databaseConnection, configuration.toServiceId()) {

    override val fileTemplateRepository: NodeFileTemplateRepository
    override val serverVersionTypeRepository: CloudServerVersionTypeRepository
    override val moduleHandler: ModuleHandler
    val console: NodeConsole
    val fileNodeRepository: FileNodeRepository
    val fileCluster: FileCluster
    val serverFactory: ServerFactory

    init {
        console = NodeConsole(configuration, eventManager, nodeRepository, serverRepository)
        fileNodeRepository = FileNodeRepository(databaseConnection, packetManager)
        fileCluster = FileCluster(serviceId, configuration.hostAddress, fileNodeRepository, packetManager, nodeRepository, eventManager)
        fileTemplateRepository = NodeFileTemplateRepository(databaseConnection, nodeRepository, fileCluster, packetManager)
        serverVersionTypeRepository = CloudServerVersionTypeRepository(databaseConnection, console, packetManager)
        serverFactory = ServerFactory(databaseConnection, nodeRepository, serverRepository, serverVersionRepository, serverVersionTypeRepository, fileTemplateRepository, javaVersionRepository, packetManager, configuration.hostAddress, console, clusterConfiguration, configurationTemplateRepository, eventManager, fileCluster)
        moduleHandler = ModuleHandler(serviceId, loadModuleRepositoryUrls(), eventManager, packetManager, null)

        runBlocking {
            registerDefaults()
            this@NodeService.initShutdownHook()

            nodeRepository.connect(this@NodeService)
            try { memoryCheck() } catch (e: Exception) {
                LOGGER.severe("Error while checking memory", e)
                shutdown()
                return@runBlocking
            }

            try { this@NodeService.checkJavaVersions() } catch (e: Exception) {
                LOGGER.warning("Error while checking java versions", e)
            }

            IServerVersionHandler.registerHandler(URLServerVersionHandler(serviceId, serverVersionRepository, serverVersionTypeRepository, nodeRepository, console, javaVersionRepository))

            this@NodeService.registerPreTasks()
            this@NodeService.connectFileCluster()
            this@NodeService.registerPackets()
            this@NodeService.registerCommands()
            this@NodeService.registerTasks()

            initApi()
            moduleHandler.loadModules()
        }
    }

    override fun shutdown(force: Boolean) {
        if (SHUTTINGDOWN && !force) return
        SHUTTINGDOWN = true
        LOGGER.info("Shutting down node service...")
        runBlocking {
            IServerVersionHandler.CACHE_HANDLERS.forEach {
                it.shutdown(force, serverVersionRepository)
            }
            serverFactory.shutdown()
            fileCluster.disconnect(true)
            nodeRepository.shutdownAction.run()
            super.shutdown(force)
            TEMP_FOLDER.getFile().deleteRecursively()
        }
    }

    private fun registerTasks() {
        taskManager.builder()
            .task(NodePingTask(this))
            .instant()
            .event(NodeDisconnectEvent::class)
            .period(10.seconds)
            .register()
        taskManager.builder()
            .task(NodeSelfSuspendTask(this))
            .event(NodeSuspendedEvent::class)
            .period(10.seconds)
            .register()
        taskManager.builder()
            .task(CloudServerStartTask(this.serverFactory, this.eventManager, this.nodeRepository, this.serverRepository))
            .event(NodeConnectEvent::class)
            .period(3.seconds)
            .register()
        taskManager.builder()
            .task(CloudAutoStartServerTask(this.configurationTemplateRepository, this.serverRepository, this.serverFactory, this.nodeRepository))
            .event(CloudServerDisconnectedEvent::class)
            .period(5.seconds)
            .register()
        taskManager.builder()
            .task(CloudServerStopTask(this.serviceId, this.serverRepository, this.serverFactory, this.configurationTemplateRepository, this.nodeRepository))
            .period(2.seconds)
            .register()
        taskManager.builder()
            .task(CloudServerQueueCleanerTask(this.serverFactory, this.nodeRepository, this.serverRepository))
            .event(NodeConnectEvent::class)
            .event(NodeDisconnectEvent::class)
            .event(NodeSuspendedEvent::class)
            .period(5.seconds)
            .register()
        taskManager.builder()
            .task(CloudServerTransferTask(this.serverFactory))
            .period(7.seconds)
            .register()
        taskManager.builder()
            .task(CloudServerDeleteTask(this.serverFactory))
            .period(5.seconds)
            .register()
        taskManager.builder()
            .task(CloudServerVersionUpdateTask(firstStart, this.serverVersionRepository, this.serverVersionTypeRepository))
            .period(5.minutes)
            .instant()
            .register()
        taskManager.builder()
            .task(MetricsTask(this.clusterConfiguration, this.serviceId, this.playerRepository, this.serverRepository))
            .instant()
            .delay(55.seconds)
            .period(5.minutes)
            .register()
    }

    private fun registerPreTasks() {
        taskManager.builder()
            .task(NodeChooseMasterTask(serviceId, nodeRepository, eventManager))
            .instant()
            .event(NodeDisconnectEvent::class)
            .event(NodeSuspendedEvent::class)
            .register()
    }

    private suspend fun checkJavaVersions() {
        val detected = mutableListOf<CloudJavaVersion>()
        javaVersionRepository.detectInstalledVersions().forEach {
            if (javaVersionRepository.existsVersion(it.name)) return@forEach
            javaVersionRepository.createVersion(it)
            detected.add(it)
        }
        if (detected.isNotEmpty()) {
            LOGGER.info("Detected %hc%${detected.size} %tc%java versions§8: %hc%${detected.joinToString("§8, %hc%") { it.name }}")
        }
        var wrongAutoDetectPossible = false
        javaVersionRepository.getVersions().forEach { version ->
            val located = version.autoLocate()
            if (located != null) {
                if (located.absolutePath == version.located[configuration.toServiceId().id]) return@forEach
                version.located[configuration.toServiceId().id] = located.absolutePath
                javaVersionRepository.updateVersion(version)
                return@forEach
            }
            if (!version.onlineVersion) {
                LOGGER.warning("Java version §8'%tc%${version.name}§8'%tc% is not installed!")
                wrongAutoDetectPossible = true
            }
        }
        if (wrongAutoDetectPossible) {
            LOGGER.warning("§cIf the version is installed, try to set the java home manually with '-Dredicloud.java.versions.path=path/to/java/versions'")
        }
    }

    private suspend fun memoryCheck() {
        val thisNode = nodeRepository.getNode(this.serviceId)!!
        if (thisNode.maxMemory < 1024) throw IllegalStateException("Max memory of this node is too low! Please increase the max memory of this node!")
        if (thisNode.maxMemory > Runtime.getRuntime().freeMemory()) throw IllegalStateException("Not enough memory available! Please increase the max memory of this node!")
    }

    private fun registerPackets() {}

    private suspend fun connectFileCluster() {
        try {
            this.fileCluster.connect()
            LOGGER.info("Connected to file cluster on port ${this.fileCluster.port}!")
        }catch (e: Exception) {
            LOGGER.severe("Failed to connect to file cluster!", e)
            this.shutdown()
            return
        }
    }

    private fun registerCommands() {
        fun register(command: ICommand) {
            console.commandManager.registerCommand(command)
        }
        register(ExitCommand(this))
        register(VersionCommand())
        register(ClusterCommand(this))
        register(CloudServerVersionCommand(this.serverVersionRepository, this.serverVersionTypeRepository, this.configurationTemplateRepository, this.serverRepository, this.javaVersionRepository, this.console))
        register(CloudServerVersionTypeCommand(this.serverVersionTypeRepository, this.configurationTemplateRepository, this.serverVersionRepository))
        register(JavaVersionCommand(this.javaVersionRepository, this.serverVersionRepository))
        register(ClearCommand(this.console))
        register(ConfigurationTemplateCommand(this.configurationTemplateRepository, this.javaVersionRepository, this.serverRepository, this.serverVersionRepository, this.nodeRepository, this.fileTemplateRepository))
        register(FileTemplateCommand(this.fileTemplateRepository))
        register(ServerCommand(this.serverFactory, this.serverRepository, this.nodeRepository))
        register(ScreenCommand(this.console))
        register(ModuleCommand(this.moduleHandler, this.clusterConfiguration))
    }

    private fun initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread { this.shutdown() })
    }

    override fun configure() {
        super.configure()
        bind(ICommandManager::class).toInstance(console.commandManager)
        bind(Console::class).toInstance(console)
        bind(ICloudRemoteServerFactory::class).toInstance(serverFactory)
    }

}