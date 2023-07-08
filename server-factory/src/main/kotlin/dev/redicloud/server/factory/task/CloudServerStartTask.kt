package dev.redicloud.server.factory.task

import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.server.factory.*
import dev.redicloud.service.base.events.node.NodeConnectEvent
import dev.redicloud.service.base.events.node.NodeDisconnectEvent
import dev.redicloud.service.base.events.node.NodeSuspendedEvent
import dev.redicloud.tasks.CloudTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

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
            serverFactory.startQueue.forEach queue@{ info ->
                info.failedStarts.removeFails(it.serviceId)
                if (info.configurationTemplate.nodeIds.isNotEmpty() && !info.configurationTemplate.nodeIds.contains(it.serviceId)) {
                    info.failedStarts.addFailedStart(it.serviceId, StartResultType.NODE_IS_NOT_ALLOWED)
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
            serverFactory.startQueue.forEach queue@{ info ->
                info.failedStarts.addFailedStart(it.serviceId, StartResultType.NODE_NOT_CONNECTED)
                info.calculateStartOrder(nodes)
            }
        }
    }

    private val onNodeSuspend = eventManager.listen<NodeSuspendedEvent> {
        scope.launch {
            val nodes = nodeRepository.getConnectedNodes()
            val master = nodes.firstOrNull { it.master }
            if (master?.serviceId != nodeRepository.serviceId) return@launch
            serverFactory.startQueue.forEach queue@{ info ->
                info.failedStarts.addFailedStart(it.serviceId, StartResultType.NODE_IS_NOT_ALLOWED)
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
                serverFactory.getStartList().forEach { info ->
                    if (!info.isNextNode(nodeRepository.serviceId)) return@forEach
                    serverFactory.startQueue.remove(info)
                    val template = info.configurationTemplate
                    val result = serverFactory.startServer(template)
                    when (result.type) {

                        StartResultType.ALREADY_RUNNING -> {
                            serverFactory.startQueue.remove(info)
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.ALREADY_RUNNING)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.remove(info)
                            result as AlreadyRunningStartResult
                            logger.severe("§cServer ${result.server.getIdentifyingName(false)} was removed from the start queue because it is already running!")
                        }

                        StartResultType.RAM_USAGE_TOO_HIGH -> {
                            serverFactory.startQueue.remove(info)
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.RAM_USAGE_TOO_HIGH)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.add(info)
                            logger.warning("§cCan´t start server of template '${template.name}' because the ram usage is too high!")
                        }

                        StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE -> {
                            serverFactory.startQueue.remove(info)
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE)
                            if (result is TooMuchServicesOfTemplateOnNodeStartResult) {
                                info.addFailedNode(nodeRepository.serviceId)
                                serverFactory.startQueue.add(info)
                                logger.warning("§cCan´t start server of template '${template.name}' on this node because there are too much services of this template!")
                            }

                        }

                        StartResultType.UNKNOWN_SERVER_VERSION -> {
                            serverFactory.startQueue.remove(info)
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_SERVER_VERSION)
                            info.addFailedNode(nodeRepository.serviceId)
                            logger.warning("§cCan´t start server of template '${template.name}' because the server version is not set!")
                        }

                        StartResultType.NODE_IS_NOT_ALLOWED -> {
                            serverFactory.startQueue.remove(info)
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.NODE_IS_NOT_ALLOWED)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.add(info)
                        }

                        StartResultType.NODE_NOT_CONNECTED -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.NODE_NOT_CONNECTED)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.remove(info)
                            serverFactory.startQueue.add(info)
                        }

                        StartResultType.UNKNOWN_JAVA_VERSION -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_JAVA_VERSION)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.remove(info)
                            logger.severe("§cCan´t start server of template '${template.name}' because the java version is not set!")
                        }

                        StartResultType.JAVA_VERSION_NOT_INSTALLED -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.JAVA_VERSION_NOT_INSTALLED)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.remove(info)
                            result as JavaVersionNotInstalledStartResult
                            logger.severe("§cCan´t start server of template '${template.name}' because the java version '${result.javaVersion.name} is not installed!")
                        }

                        StartResultType.UNKNOWN_SERVER_TYPE_VERSION -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_SERVER_TYPE_VERSION)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.remove(info)
                            logger.severe("§cCan´t start server of template '${template.name}' because the server type version is not set!")
                        }

                        StartResultType.UNKNOWN_CONFIGURATION_TEMPLATE -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_CONFIGURATION_TEMPLATE)
                            serverFactory.startQueue.remove(info)
                            logger.severe("§cCan´t start static server because the configuration template is unknown?!")
                        }

                        StartResultType.UNKNOWN_ERROR -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_ERROR)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.remove(info)
                            serverFactory.startQueue.add(info)
                            val errorResult = result as UnknownErrorStartResult
                            logger.severe(
                                "§cAn unknown error occurred while starting server of template '${template.name}'!",
                                errorResult.throwable
                            )
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                logger.severe("§cAn unknown error occurred while starting servers!", e)
            } finally {
                nextTick = true
            }
        }
        while (!nextTick) Thread.sleep(200)
        return false
    }

}