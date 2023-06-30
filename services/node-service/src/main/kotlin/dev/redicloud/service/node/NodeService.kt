package dev.redicloud.service.node

import dev.redicloud.cluster.file.FileCluster
import dev.redicloud.cluster.file.FileNodeRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.base.events.NodeDisconnectEvent
import dev.redicloud.service.base.events.NodeSuspendedEvent
import dev.redicloud.service.node.commands.ClusterCommand
import dev.redicloud.service.node.commands.ExitCommand
import dev.redicloud.service.node.console.NodeConsole
import dev.redicloud.service.node.repository.node.connect
import dev.redicloud.service.node.repository.node.disconnect
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.service.node.commands.CloudServerVersionCommand
import dev.redicloud.service.node.tasks.node.NodeChooseMasterTask
import dev.redicloud.service.node.tasks.NodePingTask
import dev.redicloud.service.node.tasks.NodeSelfSuspendTask
import dev.redicloud.utils.TEMP_FOLDER
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class NodeService(
    databaseConfiguration: DatabaseConfiguration,
    databaseConnection: DatabaseConnection,
    val configuration: NodeConfiguration
) : BaseService(databaseConfiguration, databaseConnection, configuration.toServiceId()) {

    val console: NodeConsole = NodeConsole(configuration, eventManager, nodeRepository)
    val fileNodeRepository: FileNodeRepository
    val fileCluster: FileCluster

    init {
        fileNodeRepository = FileNodeRepository(databaseConnection, packetManager)
        fileCluster = FileCluster(configuration.hostAddress, fileNodeRepository, packetManager, nodeRepository, eventManager)
        runBlocking {
            this@NodeService.initShutdownHook()

            nodeRepository.connect(this@NodeService)
            try { memoryCheck() } catch (e: Exception) {
                LOGGER.severe("Error while checking memory", e)
                shutdown()
                return@runBlocking
            }

            this@NodeService.registerPreTasks()
            this@NodeService.connectFileCluster()
            this@NodeService.registerPackets()
            this@NodeService.registerCommands()
            this@NodeService.registerTasks()
        }
    }

    override fun shutdown() {
        if (SHUTTINGDOWN) return
        SHUTTINGDOWN = true
        LOGGER.info("Shutting down node service...")
        runBlocking {
            fileCluster.disconnect(true)
            nodeRepository.disconnect(this@NodeService)
            super.shutdown()
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
    }

    private fun registerPreTasks() {
        taskManager.builder()
            .task(NodeChooseMasterTask(nodeRepository))
            .instant()
            .event(NodeDisconnectEvent::class)
            .event(NodeSuspendedEvent::class)
            .register()
    }

    private suspend fun memoryCheck() {
        val thisNode = nodeRepository.getNode(this.serviceId)!!
        if (thisNode.maxMemory < 1024) throw IllegalStateException("Max memory of this node is too low! Please increase the max memory of this node!")
        if (thisNode.maxMemory > Runtime.getRuntime().freeMemory()) throw IllegalStateException("Not enough memory available! Please increase the max memory of this node!")
    }

    private fun registerPackets() {
    }

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
        console.commandManager.register(ExitCommand(this))
        console.commandManager.register(ClusterCommand(this))
        console.commandManager.register(CloudServerVersionCommand(this.serverVersionRepository, this.configurationTemplateRepository, this.serverRepository, this.console))
    }

    private fun initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread { this.shutdown() })
    }

}