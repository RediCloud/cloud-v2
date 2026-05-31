package dev.redicloud.server.factory.task

import dev.redicloud.api.events.internal.node.NodeConnectEvent
import dev.redicloud.api.events.internal.node.NodeDisconnectEvent
import dev.redicloud.api.events.internal.node.NodeSuspendedEvent
import dev.redicloud.api.events.listen
import dev.redicloud.api.utils.factory.*
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.server.factory.*
import dev.redicloud.server.factory.utils.*
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.ConcurrentBatch
import dev.redicloud.utils.coroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

class CloudServerStartTask(
    private val serverFactory: ServerFactory,
    eventManager: EventManager,
    private val nodeRepository: NodeRepository,
    private val serverRepository: ServerRepository
) : CloudTask() {

    init {
        eventManager.listen<NodeConnectEvent> {
            scope.launch {
                val nodes = nodeRepository.getConnectedNodes()
                val master = nodes.firstOrNull { it.master }
                if (master?.serviceId != serverFactory.hostingId) return@launch
                serverFactory.startQueue.forEach queue@{ info ->
                    info.failedStarts.removeFails(it.serviceId)
                    info.calculateStartOrder(nodes, serverRepository)
                }
            }
        }

        eventManager.listen<NodeDisconnectEvent> {
            scope.launch {
                val nodes = nodeRepository.getConnectedNodes()
                val master = nodes.firstOrNull { it.master }
                if (master?.serviceId != serverFactory.hostingId) return@launch
                serverFactory.startQueue.forEach queue@{ info ->
                    info.failedStarts.addFailedStart(it.serviceId, StartResultType.NODE_NOT_CONNECTED)
                    info.calculateStartOrder(nodes, serverRepository)
                }
            }
        }

        eventManager.listen<NodeSuspendedEvent> {
            scope.launch {
                val nodes = nodeRepository.getConnectedNodes()
                val master = nodes.firstOrNull { it.master }
                if (master?.serviceId != serverFactory.hostingId) return@launch
                serverFactory.startQueue.forEach queue@{ info ->
                    info.failedStarts.addFailedStart(it.serviceId, StartResultType.NODE_IS_NOT_ALLOWED)
                    info.calculateStartOrder(nodes, serverRepository)
                }
            }
        }
    }

    companion object {
        private val logger = LogManager.logger(CloudServerStartTask::class)

        @OptIn(DelicateCoroutinesApi::class)
        private val scope = CoroutineScope(SupervisorJob() + newSingleThreadContext("server-factory-start") + coroutineExceptionHandler)
    }

    override suspend fun execute(): Boolean {
        val actions = ConcurrentBatch()
        serverFactory.getStartList().forEach { info ->
            if (!info.isNextNode(serverFactory.hostingId)) return@forEach
            val name = if (info.serviceId == null) info.configurationTemplate.name else info.serviceId?.toName() ?: "unknown"
            actions.add {
                @Suppress("TooGenericExceptionCaught")
                try {
                    serverFactory.startQueue.remove(info)
                    val result = if (info.serviceId == null) {
                        serverFactory.startServer(info.configurationTemplate, serverUniqueId = info.uniqueId)
                    } else {
                        serverFactory.startServer(info.serviceId, null)
                    }
                    processStartResult(result, info, name)
                } catch (e: Exception) {
                    logger.severe("§cAn error occurred while starting server ${toConsoleValue(name, false)}!", e)
                }
            }
        }
        actions.joinAll()
        return false
    }

    private fun processStartResult(result: StartResult, info: ServerQueueInformation, name: String) {
        when (result.type) {
            StartResultType.ALREADY_RUNNING -> {
                markFailedAndRemove(info, StartResultType.ALREADY_RUNNING)
                result as AlreadyRunningStartResult
                logger.severe(
                    "§cServer ${result.server.identifyName(
                        false
                    )} was removed from the start queue because it is already running!"
                )
            }
            StartResultType.RAM_USAGE_TOO_HIGH -> {
                markFailedAndRequeue(info, StartResultType.RAM_USAGE_TOO_HIGH)
                logger.warning("§cCan´t start server ${toConsoleValue(name, false)} because the ram usage is too high!")
            }
            StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE -> {
                markFailed(info, StartResultType.TOO_MUCH_SERVICES_OF_TEMPLATE)
                if (result is TooMuchServicesOfTemplateOnNodeStartResult) {
                    info.addFailedNode(serverFactory.hostingId)
                    serverFactory.startQueue.add(info)
                    logger.warning(
                        "§cCan´t start server ${toConsoleValue(
                            name,
                            false
                        )} on this node because there are too much services of this template!"
                    )
                }
            }
            StartResultType.UNKNOWN_SERVER_VERSION -> {
                markFailedAndRemove(info, StartResultType.UNKNOWN_SERVER_VERSION)
                logger.warning(
                    "§cCan´t start server ${toConsoleValue(name, false)} because the server version is not set!"
                )
            }
            StartResultType.NODE_IS_NOT_ALLOWED -> markFailedAndRequeue(info, StartResultType.NODE_IS_NOT_ALLOWED)
            StartResultType.NODE_NOT_CONNECTED -> markFailedAndRequeue(info, StartResultType.NODE_NOT_CONNECTED)
            StartResultType.UNKNOWN_JAVA_VERSION -> {
                markFailedAndRemove(info, StartResultType.UNKNOWN_JAVA_VERSION)
                logger.severe(
                    "§cCan´t start server ${toConsoleValue(name, false)} because the java version is not set!"
                )
            }
            StartResultType.JAVA_VERSION_NOT_INSTALLED -> {
                markFailedAndRemove(info, StartResultType.JAVA_VERSION_NOT_INSTALLED)
                result as JavaVersionNotInstalledStartResult
                logger.severe(
                    "§cCan´t start server ${toConsoleValue(
                        name,
                        false
                    )} because the java version '${result.javaVersion.name} is not installed!"
                )
            }
            StartResultType.UNKNOWN_SERVER_TYPE_VERSION -> {
                markFailedAndRemove(info, StartResultType.UNKNOWN_SERVER_TYPE_VERSION)
                logger.severe(
                    "§cCan´t start server ${toConsoleValue(name, false)} because the server type version is not set!"
                )
            }
            StartResultType.UNKNOWN_CONFIGURATION_TEMPLATE -> {
                markFailed(info, StartResultType.UNKNOWN_CONFIGURATION_TEMPLATE)
                serverFactory.startQueue.remove(info)
                logger.severe(
                    "§cCan´t start static server ${toConsoleValue(
                        name,
                        false
                    )} because the configuration template is unknown?!"
                )
            }
            StartResultType.UNKNOWN_ERROR -> {
                markFailedAndRequeue(info, StartResultType.UNKNOWN_ERROR)
                val errorResult = result as UnknownErrorStartResult
                logger.severe(
                    "§cAn unknown error occurred while starting server ${toConsoleValue(name, false)}!",
                    errorResult.throwable
                )
            }
            else -> {}
        }
    }

    private fun markFailed(info: ServerQueueInformation, type: StartResultType) {
        serverFactory.startQueue.remove(info)
        info.addFailedStart(serverFactory.hostingId, type)
    }

    private fun markFailedAndRemove(info: ServerQueueInformation, type: StartResultType) {
        markFailed(info, type)
        info.addFailedNode(serverFactory.hostingId)
    }

    private fun markFailedAndRequeue(info: ServerQueueInformation, type: StartResultType) {
        markFailed(info, type)
        info.addFailedNode(serverFactory.hostingId)
        serverFactory.startQueue.add(info)
    }
}
