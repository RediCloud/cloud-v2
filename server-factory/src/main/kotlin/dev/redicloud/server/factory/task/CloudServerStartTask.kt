package dev.redicloud.server.factory.task

import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.server.factory.*
import dev.redicloud.service.base.events.NodeConnectEvent
import dev.redicloud.service.base.events.NodeDisconnectEvent
import dev.redicloud.service.base.events.NodeSuspendedEvent
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.ioScope
import kotlinx.coroutines.*

class CloudServerStartTask(
    private val serverFactory: ServerFactory,
    private val eventManager: EventManager,
    private val nodeRepository: NodeRepository
) : CloudTask() {

    private val onNodeConnect = eventManager.listen<NodeConnectEvent> {
        scope.launch {
            val nodes = nodeRepository.getConnectedNodes()
            val master = nodes.firstOrNull { it.master }
            if (master?.serviceId != nodeRepository.serviceId) return@launch
            serverFactory.startQueue.forEach queue@{ id, info ->
                info.failedStarts.forEach { serviceId, map ->
                    // Remove the node from the failed starts
                    if (serviceId != it.serviceId) return@forEach
                    if (info.configurationTemplate.nodeIds.isNotEmpty() && !info.configurationTemplate.nodeIds.contains(serviceId)) return@forEach
                    map.remove(StartResultType.NODE_NOT_CONNECTED)
                    map.remove(StartResultType.RAM_USAGE_TOO_HIGH)
                    map.remove(StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE)
                }
                info.calculateStartOrder(nodes)
            }
        }
    }

    private val onNodeDisconnect = eventManager.listen<NodeDisconnectEvent> {
        scope.launch {
            val nodes = nodeRepository.getConnectedNodes()
            val master = nodes.firstOrNull { it.master }
            if (master?.serviceId != nodeRepository.serviceId) return@launch
            serverFactory.startQueue.forEach queue@{ id, info ->
                info.failedStarts.forEach { serviceId, map ->
                    // Remove the node from the failed starts
                    if (serviceId != it.serviceId) return@forEach
                    map[StartResultType.NODE_NOT_CONNECTED] = map.getOrDefault(StartResultType.NODE_NOT_CONNECTED, 0) + 1
                }
                info.calculateStartOrder(nodes)
            }
        }
    }

    private val onNodeSuspend = eventManager.listen<NodeSuspendedEvent> {
        scope.launch {
            val nodes = nodeRepository.getConnectedNodes()
            val master = nodes.firstOrNull { it.master }
            if (master?.serviceId != nodeRepository.serviceId) return@launch
            serverFactory.startQueue.forEach queue@{ id, info ->
                info.failedStarts.forEach { serviceId, map ->
                    // Remove the node from the failed starts
                    if (serviceId != it.serviceId) return@forEach
                    map[StartResultType.NODE_NOT_CONNECTED] = map.getOrDefault(StartResultType.NODE_NOT_CONNECTED, 0) + 1
                }
                info.calculateStartOrder(nodes)
            }
        }
    }

    companion object {
        private val logger = LogManager.logger(CloudServerStartTask::class)
        @OptIn(DelicateCoroutinesApi::class)
        private val scope = CoroutineScope(newSingleThreadContext("server-factory-start"))
    }

    override suspend fun execute(): Boolean {
        var nextTick = false
        scope.launch {
            try {
                serverFactory.getStartList().forEach {
                    if (it.isNextNode(nodeRepository.serviceId)) return@forEach
                    val info = serverFactory.startQueue.remove(it.uniqueId) ?: return@forEach
                    val template = info.configurationTemplate
                    val result = serverFactory.startServer(template)
                    when (result.type) {
                        StartResultType.ALREADY_RUNNING -> {
                            val alreadyRunningStartResult = result as AlreadyRunningStartResult
                            logger.warning("§cServer ${alreadyRunningStartResult.server.getIdentifyingName(false)} was removed from the start queue because it is already running!")
                        }
                        StartResultType.RAM_USAGE_TOO_HIGH -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.RAM_USAGE_TOO_HIGH)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue[it.uniqueId] = info
                            logger.warning("§cCan´t start server of template '${template.name}' because the ram usage is too high!")
                        }
                        StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE)
                            serverFactory.startQueue[it.uniqueId] = info
                            logger.warning("§cCan´t start server of template '${template.name}' because there are too much services of this template!")
                        }
                        StartResultType.UNKNOWN_SERVER_VERSION -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_SERVER_VERSION)
                            logger.warning("§cCan´t start server of template '${template.name}' because the server version is not set!")
                        }
                        StartResultType.UNKNOWN_ERROR -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_ERROR)
                            val errorResult = result as UnknownErrorStartResult
                            logger.severe("§cAn unknown error occurred while starting server of template '${template.name}'!", errorResult.throwable)
                        }
                        else -> {}
                    }
                }
            }catch (e: Exception) {
                logger.severe("§cAn unknown error occurred while starting servers!", e)
            }finally {
                nextTick = true
            }
        }
        while (!nextTick) Thread.sleep(200)
        return false
    }

}