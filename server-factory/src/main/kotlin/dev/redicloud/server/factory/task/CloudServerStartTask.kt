package dev.redicloud.server.factory.task

import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.server.factory.*
import dev.redicloud.server.factory.utils.*
import dev.redicloud.api.events.impl.node.NodeConnectEvent
import dev.redicloud.api.events.impl.node.NodeDisconnectEvent
import dev.redicloud.api.events.impl.node.NodeSuspendedEvent
import dev.redicloud.api.events.listen
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.MultiAsyncAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

class CloudServerStartTask(
    private val serverFactory: ServerFactory,
    eventManager: EventManager,
    private val nodeRepository: NodeRepository,
    private val serverRepository: ServerRepository
) : CloudTask() {

    private val onNodeConnect = eventManager.listen<NodeConnectEvent> {
        scope.launch {
            val nodes = nodeRepository.getConnectedNodes()
            val master = nodes.firstOrNull { it.master }
            if (master?.serviceId != nodeRepository.serviceId) return@launch
            serverFactory.startQueue.forEach queue@{ info ->
                info.failedStarts.removeFails(it.serviceId)
                info.calculateStartOrder(nodes, serverRepository)
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
                info.calculateStartOrder(nodes, serverRepository)
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
                info.calculateStartOrder(nodes, serverRepository)
            }
        }
    }

    companion object {
        private val logger = LogManager.logger(CloudServerStartTask::class)

        @OptIn(DelicateCoroutinesApi::class)
        private val scope = CoroutineScope(newSingleThreadContext("server-factory-start"))
    }

    override suspend fun execute(): Boolean {
        val actions = MultiAsyncAction()
        serverFactory.getStartList().forEach { info ->
            if (!info.isNextNode(nodeRepository.serviceId)) return@forEach

            val name = if (info.configurationTemplate != null) info.configurationTemplate.name else info.serviceId?.toName() ?: "unknown"

            actions.add {
                try {
                    serverFactory.startQueue.remove(info)
                    val result = if (info.configurationTemplate != null) {
                        serverFactory.startServer(info.configurationTemplate)
                    }else if (info.serviceId != null) {
                        serverFactory.startServer(info.serviceId, null)
                    }else null

                    if (result == null) return@add

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
                            logger.warning("§cCan´t start server ${toConsoleValue(name, false)} because the ram usage is too high!")
                        }

                        StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE -> {
                            serverFactory.startQueue.remove(info)
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE)
                            if (result is TooMuchServicesOfTemplateOnNodeStartResult) {
                                info.addFailedNode(nodeRepository.serviceId)
                                serverFactory.startQueue.add(info)
                                logger.warning("§cCan´t start server ${toConsoleValue(name, false)} on this node because there are too much services of this template!")
                            }

                        }

                        StartResultType.UNKNOWN_SERVER_VERSION -> {
                            serverFactory.startQueue.remove(info)
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_SERVER_VERSION)
                            info.addFailedNode(nodeRepository.serviceId)
                            logger.warning("§cCan´t start server ${toConsoleValue(name, false)} because the server version is not set!")
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
                            logger.severe("§cCan´t start server ${toConsoleValue(name, false)} because the java version is not set!")
                        }

                        StartResultType.JAVA_VERSION_NOT_INSTALLED -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.JAVA_VERSION_NOT_INSTALLED)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.remove(info)
                            result as JavaVersionNotInstalledStartResult
                            logger.severe("§cCan´t start server ${toConsoleValue(name, false)} because the java version '${result.javaVersion.name} is not installed!")
                        }

                        StartResultType.UNKNOWN_SERVER_TYPE_VERSION -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_SERVER_TYPE_VERSION)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.remove(info)
                            logger.severe("§cCan´t start server ${toConsoleValue(name, false)} because the server type version is not set!")
                        }

                        StartResultType.UNKNOWN_CONFIGURATION_TEMPLATE -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_CONFIGURATION_TEMPLATE)
                            serverFactory.startQueue.remove(info)
                            logger.severe("§cCan´t start static server ${toConsoleValue(name, false)} because the configuration template is unknown?!")
                        }

                        StartResultType.UNKNOWN_ERROR -> {
                            info.addFailedStart(nodeRepository.serviceId, StartResultType.UNKNOWN_ERROR)
                            info.addFailedNode(nodeRepository.serviceId)
                            serverFactory.startQueue.remove(info)
                            serverFactory.startQueue.add(info)
                            val errorResult = result as UnknownErrorStartResult
                            logger.severe(
                                "§cAn unknown error occurred while starting server ${toConsoleValue(name, false)}!",
                                errorResult.throwable
                            )
                        }

                        else -> {}
                    }
                }catch (e: Exception) {
                    logger.severe("§cAn error occurred while starting server ${toConsoleValue(name, false)}!", e)
                }
            }
        }
        actions.joinAll()
        return false
    }

}