package dev.redicloud.server.factory.task

import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.utils.MultiAsyncAction
import dev.redicloud.api.service.ServiceId
import dev.redicloud.repository.template.configuration.ConfigurationTemplate

class CloudServerStopTask(
    private val serviceId: ServiceId,
    private val serverRepository: ServerRepository,
    private val serverFactory: ServerFactory,
    private val configurationTemplateRepository: ConfigurationTemplateRepository,
    private val nodeRepository: NodeRepository
) : AbstractCloudFactoryTask(serverFactory) {

    companion object {
        private val logger = LogManager.logger(CloudServerStopTask::class)
    }

    override suspend fun execute(): Boolean {
        processRequestedServerStops()

        val thisNode = nodeRepository.getNode(serviceId)!!

        if (!thisNode.master) return false

        val actions = MultiAsyncAction()
        val servers = serverRepository.getConnectedServers()
            .filter { it.state == CloudServerState.RUNNING }
            .filter { !it.hidden }
            .filter { it.currentSession != null }
            .filter { it.connected }

        val templates = configurationTemplateRepository.getTemplates()
        val nodes = nodeRepository.getConnectedNodes()
        val startedServers = getStartedServers(nodes, servers, templates)

        templates.forEach { template ->
            val templateBasedServers = servers.filter { it.configurationTemplate.uniqueId == template.uniqueId }
            val stopAble = templateBasedServers.filter { it.connectedPlayers.isEmpty() }
                .filter { template.timeAfterStopUselessServer < System.currentTimeMillis() - it.currentSession!!.startTime }
                .toMutableList()

            val templateStartedServers = startedServers.getOrDefault(template, emptyMap())
            val globalStarted = templateStartedServers.values.sum()

            actions.add {
                requestGlobalServerStops(template, globalStarted, stopAble)
            }

            actions.add {
                requestNodeServerStops(template, templateStartedServers, stopAble)
            }
        }
        actions.joinAll()
        return false
    }

    private suspend fun requestNodeServerStops(template: ConfigurationTemplate, templateStartedServers: Map<ServiceId, Int>, stopAble: MutableList<CloudServer>) {
        val actions = MultiAsyncAction()
        templateStartedServers.forEach { (nodeId, count) ->
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
        actions.joinAll()
    }

    private suspend fun requestGlobalServerStops(template: ConfigurationTemplate, started: Int, stopAble: MutableList<CloudServer>) {
        if (template.minStartedServices !in 1 until started) return
        if (stopAble.isEmpty()) return

        val actions = MultiAsyncAction()
        val countToStop = started - template.minStartedServices
        stopAble.take(countToStop).forEach {
            actions.add {
                serverFactory.queueStop(it.serviceId)
            }
        }
        actions.joinAll()
    }

    private suspend fun processRequestedServerStops() {
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
        actions.joinAll()
    }

}