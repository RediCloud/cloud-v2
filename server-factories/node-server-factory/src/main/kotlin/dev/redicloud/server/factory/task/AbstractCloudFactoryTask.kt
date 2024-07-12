package dev.redicloud.server.factory.task

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask
import java.util.*

abstract class AbstractCloudFactoryTask(
    private val serverFactory: ServerFactory
) : CloudTask() {

    fun getNodeServerStarts(
        nodeBasedStarts: Map<ConfigurationTemplate, Map<ServiceId, Int>>
    ): Map<ServiceId, Map<ConfigurationTemplate, Int>> {
        val nodeServerStarts: MutableMap<ServiceId, MutableMap<ConfigurationTemplate, Int>> = mutableMapOf()
        nodeBasedStarts.forEach { (template, nodeStarts) ->
            val minServersPerNode = template.minStartedServicesPerNode

            if (getUnassignedQueuedServerStarts().isNotEmpty() || minServersPerNode < 1) return@forEach
            if (nodeStarts.values.none { it < minServersPerNode }) return@forEach

            val starts: MutableMap<ServiceId, MutableMap<ConfigurationTemplate, Int>> = mutableMapOf()
            nodeStarts.forEach { (nodeId, count) ->
                if (count >= minServersPerNode) return@forEach
                val amountToStart = minServersPerNode - count
                starts.getOrDefault(nodeId, mutableMapOf())[template] = amountToStart
            }
        }
        return nodeServerStarts
    }

    suspend fun queueServerStart(
        template: ConfigurationTemplate,
        amount: Int,
        registeredServers: List<CloudServer>,
        nodeTargetId: ServiceId? = null
    ) {
        if (!template.static) {
            serverFactory.queueStart(template, amount, nodeTargetId)
            return
        }
        val targetStaticServers = registeredServers
            .filter { it.configurationTemplate.uniqueId == template.uniqueId }
            .filter { it.state == CloudServerState.STOPPED }
            .toMutableList()
        for (i in 0 until amount) {
            if (targetStaticServers.isNotEmpty()) {
                val targetServer = targetStaticServers.drop(1).first()
                serverFactory.queueStart(targetServer.serviceId)
            } else {
                serverFactory.queueStart(template, 1, nodeTargetId)
            }
        }
    }

    /**
     * @param nodes map of nodes and how many servers are started on them
     */
    fun getStartedServers(
        nodes: List<CloudNode>,
        targetServers: List<CloudServer>,
        templates: List<ConfigurationTemplate>
    ): Map<ConfigurationTemplate, Map<ServiceId, Int>> {
        val nodeBasedStarts: MutableMap<ConfigurationTemplate, MutableMap<ServiceId, Int>> = mutableMapOf()
        templates.forEach { template ->
            val templateStarts: MutableMap<ServiceId, Int> = mutableMapOf()
            nodes.forEach { node ->
                templateStarts[node.serviceId] = 0
            }
            targetServers.filter { it.configurationTemplate.uniqueId == template.uniqueId }
                .filter { it.state != CloudServerState.STOPPED }
                .filter { !it.hidden }
                .forEach {
                    val count = templateStarts.getOrDefault(it.hostNodeId, 0)
                    templateStarts[it.hostNodeId] = count + 1
                }
            serverFactory.startQueue.forEach { queueInfo ->
                if (queueInfo.nodeTarget != null && queueInfo.configurationTemplate.uniqueId == template.uniqueId) {
                    val count = templateStarts.getOrDefault(queueInfo.nodeTarget, 0)
                    templateStarts[queueInfo.nodeTarget!!] = count + 1
                }
            }
        }
        return nodeBasedStarts
    }

    /**
     * @return map of configuration templates and how many servers are queued to start
     */
    fun getUnassignedQueuedServerStarts(): Map<UUID, Int> {
        val unassigned = mutableMapOf<UUID, Int>()
        serverFactory.startQueue.forEach { queueInfo ->
            val count = unassigned.getOrDefault(queueInfo.configurationTemplate.uniqueId, 0)
            unassigned[queueInfo.configurationTemplate.uniqueId] = count + 1
        }
        return unassigned
    }

}