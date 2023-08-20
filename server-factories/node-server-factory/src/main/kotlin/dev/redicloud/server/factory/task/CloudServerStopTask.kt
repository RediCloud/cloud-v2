package dev.redicloud.server.factory.task

import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.MultiAsyncAction
import dev.redicloud.api.service.ServiceId

class CloudServerStopTask(
    private val serviceId: ServiceId,
    private val serverRepository: ServerRepository,
    private val serverFactory: ServerFactory,
    private val configurationTemplateRepository: ConfigurationTemplateRepository,
    private val nodeRepository: NodeRepository
) : CloudTask() {

    companion object {
        private val logger = LogManager.logger(CloudServerStopTask::class)
    }

    override suspend fun execute(): Boolean {
        val actions = MultiAsyncAction()
        serverFactory.stopQueue.forEach {
            actions.add {
                try {
                    val server = serverRepository.getServer<CloudServer>(it)
                    if (server == null) {
                        serverFactory.stopQueue.remove(it)
                        return@add
                    }
                    if (server.hostNodeId == serviceId) {
                        serverFactory.stopQueue.remove(it)
                        serverFactory.stopServer(it)
                    }
                } catch (e: Exception) {
                    logger.severe("Failed to stop server ${it.toName()}", e)
                }
            }
        }

        val thisNode = nodeRepository.getNode(serviceId)!!
        if (thisNode.master) {
            val servers = serverRepository.getConnectedServers()
                .filter { it.state == CloudServerState.RUNNING }
                .filter { !it.hidden }
                .filter { it.currentSession != null }
                .filter { it.connected }

            configurationTemplateRepository.getTemplates().forEach { template ->
                val templateBasedServers = servers.filter { it.configurationTemplate.uniqueId == template.uniqueId }
                val nodeBasedServers = mutableMapOf<ServiceId, Int>()
                templateBasedServers.forEach {
                    if (nodeBasedServers.containsKey(it.hostNodeId)) {
                        nodeBasedServers[it.hostNodeId] = nodeBasedServers[it.hostNodeId]!! + 1
                    } else {
                        nodeBasedServers[it.hostNodeId] = 1
                    }
                }
                val stopAble = templateBasedServers.filter { it.connectedPlayers.isEmpty() }
                    .filter { template.timeAfterStopUselessServer < System.currentTimeMillis() - it.currentSession!!.startTime }
                val global = nodeBasedServers.values.sum()

                if (template.minStartedServices in 1 until global) {
                    val countToStop = global - template.minStartedServices
                    stopAble.take(countToStop).forEach {
                        actions.add {
                            serverFactory.queueStop(it.serviceId)
                        }
                    }
                    return@forEach
                }
                nodeBasedServers.forEach { (nodeId, count) ->
                    if (template.minStartedServicesPerNode in 1 until count) {
                        val countToStop = count - template.minStartedServicesPerNode
                        stopAble.filter { it.hostNodeId == nodeId }.take(countToStop).forEach {
                            actions.add {
                                serverFactory.queueStop(it.serviceId)
                            }
                        }
                        return@forEach
                    }
                }
            }
        }

        actions.joinAll()
        return false
    }
}