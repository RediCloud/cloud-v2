package dev.redicloud.service.node

import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.node.console.NodeConsole
import dev.redicloud.service.node.events.NodeDisconnectEvent
import dev.redicloud.service.node.events.NodeSuspendEvent
import dev.redicloud.service.node.repository.connect
import dev.redicloud.service.node.repository.disconnect
import dev.redicloud.service.node.tasks.NodeChooseMasterTask
import dev.redicloud.service.node.tasks.NodePingTask
import dev.redicloud.service.node.tasks.NodeSelfSuspendTask
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class NodeService(databaseConfiguration: DatabaseConfiguration, val configuration: NodeConfiguration)
    : BaseService(databaseConfiguration, configuration.toServiceId()) {

    private val console: NodeConsole = NodeConsole(configuration, eventManager)

    init {
        runBlocking {
            initShutdownHook()

            nodeRepository.connect(this@NodeService)

            registerTasks()
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
            .event(NodeDisconnectEvent::class)
            .event(NodeSuspendEvent::class)
            .register()
        taskManager.builder()
            .task(NodePingTask(this))
            .event(NodeDisconnectEvent::class)
            .period(10.seconds)
            .register()
        taskManager.builder()
            .task(NodeSelfSuspendTask(this))
            .event(NodeSuspendEvent::class)
            .period(10.seconds)
            .register()
    }

    private fun initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread { this.shutdown()})
    }

}