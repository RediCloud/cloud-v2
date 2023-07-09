package dev.redicloud.server.factory.task

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask
import kotlin.time.Duration.Companion.minutes

class CloudServerQueueCleanerTask(
    private val serverFactory: ServerFactory,
    private val nodeRepository: NodeRepository,
    private val serverRepository: ServerRepository
) : CloudTask() {

    companion object {
        private val logger = LogManager.logger(CloudServerQueueCleanerTask::class)
        val MAX_QUEUE_TIME = System.getProperty("redicloud.server.factory.max-queue-time", 3.minutes.inWholeMilliseconds.toString()).toLong()

    }

    override suspend fun execute(): Boolean {
        val nodes = nodeRepository.getConnectedNodes()
        val masterNode = nodes.firstOrNull { it.master }
        if (masterNode?.serviceId != nodeRepository.serviceId) return false

        serverFactory.getStartList().forEach { info ->
            val name = if (info.configurationTemplate != null) {
                info.configurationTemplate.name
            } else if (info.serviceId != null) {
                info.serviceId.toName()
            } else null

            if (name == null) {
                logger.warning("§cCould not get name of server to start, cancelling server start!")
                serverFactory.startQueue.remove(info)
                return@forEach
            }

            // Check if queue time is too long
            if ((System.currentTimeMillis() - info.queueTime) - MAX_QUEUE_TIME > 0) {
                logger.warning("§cStart of template ${toConsoleValue(name, false)} took too long, cancelling server start!")
                serverFactory.startQueue.remove(info)
                return@forEach
            }
            // Check if no node is available
            if (info.nodeStartOrder.isEmpty()) {
                logger.warning("§cNo node for template ${toConsoleValue(name, false)} available, cancelling server start!")
                serverFactory.startQueue.remove(info)
                return@forEach
            }
        }

        serverFactory.transferQueue.forEach {
            val server = serverRepository.getServer<CloudServer>(it.serverId)
            if (server == null) {
                logger.warning("§cServer ${toConsoleValue(it.serverId.toName(), false)} does not exist, cancelling server transfer!")
                serverFactory.transferQueue.remove(it)
                return@forEach
            }
            if (server.hostNodeId == it.targetNodeId) {
                logger.warning("§cServer ${toConsoleValue(it.serverId.toName(), false)} is already on node ${toConsoleValue(it.targetNodeId.toName(), false)}, cancelling server transfer!")
                serverFactory.transferQueue.remove(it)
                return@forEach
            }
            val node = nodeRepository.getNode(it.targetNodeId)
            if (node == null) {
                logger.warning("§cNode ${toConsoleValue(it.targetNodeId.toName(), false)} does not exist, cancelling server transfer!")
                serverFactory.transferQueue.remove(it)
                return@forEach
            }
        }

        serverFactory.stopQueue.forEach {
            val server = serverRepository.getServer<CloudServer>(it)
            if (server == null) {
                logger.warning("§cServer ${toConsoleValue(it.toName(), false)} does not exist, cancelling server stop!")
                serverFactory.stopQueue.remove(it)
                return@forEach
            }
            if (server.state == CloudServerState.STOPPED) {
                logger.warning("§cServer ${toConsoleValue(it.toName(), false)} is not connected, cancelling server stop!")
                serverFactory.stopQueue.remove(it)
                return@forEach
            }
        }

        return false
    }

}