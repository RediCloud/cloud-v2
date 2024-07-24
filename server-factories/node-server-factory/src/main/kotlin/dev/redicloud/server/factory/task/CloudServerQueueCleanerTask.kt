package dev.redicloud.server.factory.task

import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.console.utils.toConsoleValue
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
        if (nodeRepository.getMasterNode()?.serviceId != serverFactory.hostingId) return false

        cleanStartQueue()
        cleanTransferQueue()
        cleanStopQueue()

        return false
    }

    private suspend fun cleanStartQueue() {
        serverFactory.getStartList().forEach { info ->
            val name = if (info.serviceId != null) {
                info.serviceId!!.toName()
            } else info.configurationTemplate.name

            if (info.serviceId != null) {
                val service = serverRepository.getServer<CloudServer>(info.serviceId!!)
                val node = nodeRepository.getNode(service!!.hostNodeId)
                if (node == null || !node.connected && info.queueTime - (MAX_QUEUE_TIME/3) > 0) {
                    logger.warning("§cNode for static service ${toConsoleValue(service.name, false)} (node-id: ${toConsoleValue(service.hostNodeId, false)}) is not connected, cancelling server start!")
                    serverFactory.startQueue.remove(info)
                    return@forEach
                }
            }

            // Check if queue time is too long
            if ((System.currentTimeMillis() - info.queueTime) - MAX_QUEUE_TIME > 0) {
                logger.warning("§cStart of template ${toConsoleValue(name, false)} took too long, cancelling server start!")
                serverFactory.startQueue.remove(info)
                return@forEach
            }
            // Check if no node is available
            if (info.nodeStartOrder.isEmpty() && info.nodeTarget == null) {
                logger.warning("§cNo node for template ${toConsoleValue(name, false)} available, cancelling server start!")
                serverFactory.startQueue.remove(info)
                return@forEach
            }
        }
    }

    private suspend fun cleanTransferQueue() {
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
    }

    private suspend fun cleanStopQueue() {
        serverFactory.stopQueue.forEach {
            val server = serverRepository.getServer<CloudServer>(it)
            if (server == null) {
                serverFactory.stopQueue.remove(it)
                return@forEach
            }
            if (server.state == CloudServerState.STOPPED) {
                logger.warning("§cServer ${toConsoleValue(it.toName(), false)} is not connected, cancelling server stop!")
                serverFactory.stopQueue.remove(it)
                return@forEach
            }
        }
    }

}