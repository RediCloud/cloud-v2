package dev.redicloud.service.node

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.node.commands.ExitCommand
import dev.redicloud.service.node.commands.ClusterCommand
import dev.redicloud.service.node.console.NodeConsole
import dev.redicloud.service.node.events.NodeDisconnectEvent
import dev.redicloud.service.node.events.NodeSuspendedEvent
import dev.redicloud.service.node.repository.connect
import dev.redicloud.service.node.repository.disconnect
import dev.redicloud.service.node.tasks.NodeChooseMasterTask
import dev.redicloud.service.node.tasks.NodePingTask
import dev.redicloud.service.node.tasks.NodeSelfSuspendTask
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class NodeService(
    databaseConfiguration: DatabaseConfiguration,
    databaseConnection: DatabaseConnection,
    val configuration: NodeConfiguration
) : BaseService(databaseConfiguration, databaseConnection, configuration.toServiceId()) {

    private val console: NodeConsole = NodeConsole(configuration, eventManager)

    companion object {
        lateinit var INSTANCE: NodeService
    }


    init {
        INSTANCE = this
        runBlocking {
            initShutdownHook()

            nodeRepository.connect(this@NodeService)

            registerTasks()
            registerCommands()
        }
    }

    override fun shutdown() {
        if (SHUTTINGDOWN) return
        SHUTTINGDOWN = true
        LOGGER.info("Shutting down node service...")
        runBlocking {
            nodeRepository.disconnect(this@NodeService)
            super.shutdown()
        }
    }

    private fun registerTasks() {
        taskManager.builder()
            .task(NodeChooseMasterTask(nodeRepository))
            .instant()
            .event(NodeDisconnectEvent::class)
            .event(NodeSuspendedEvent::class)
            .register()
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

    private fun registerCommands() {
        console.commandManager.register(ExitCommand())
        console.commandManager.register(ClusterCommand(this))
    }

    private fun initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread { this.shutdown() })
    }

}