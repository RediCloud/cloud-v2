package dev.redicloud.server.factory.task

import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.api.utils.factory.ServerQueueInformation
import dev.redicloud.tasks.CloudTask
import dev.redicloud.api.service.ServiceId
import java.util.UUID

class CloudAutoStartServerTask(
    private val configurationTemplateRepository: ConfigurationTemplateRepository,
    private val serverRepository: ServerRepository,
    private val serverFactory: ServerFactory,
    private val nodeRepository: NodeRepository
) : CloudTask() {

    override suspend fun execute(): Boolean {

        if (serverFactory.shutdown) return true

        val registeredServers = serverRepository.getRegisteredServers()
        val connectedNodes = nodeRepository.getConnectedNodes()
        configurationTemplateRepository.getTemplates().forEach { template ->
            if (serverFactory.shutdown) return true
            // Check if the template has a min start value
            if (template.minStartedServices < 1 && template.minStartedServicesPerNode < 1) return@forEach
            val nodeBasedStarts: MutableMap<ServiceId, Int> = mutableMapOf() // Map of nodes and how many servers are started on them
            val unassigned: MutableMap<UUID, Int> = mutableMapOf()
            connectedNodes.forEach { node ->
                nodeBasedStarts[node.serviceId] = 0 }
            registeredServers
                .filter { it.configurationTemplate.uniqueId == template.uniqueId }
                .filter { it.state != CloudServerState.STOPPED }
                .filter { !it.hidden }
                .forEach {
                    val count = nodeBasedStarts.getOrDefault(it.hostNodeId, 0)
                    nodeBasedStarts[it.hostNodeId] = count + 1
                }
            serverFactory.startQueue.forEach { queueInfo ->
                val t = if (queueInfo.serviceId != null) {
                    registeredServers.firstOrNull { it.serviceId == queueInfo.serviceId }?.configurationTemplate
                }else if (queueInfo.configurationTemplate != null) {
                    queueInfo.configurationTemplate
                } else null
                if (t != null) {
                    if (queueInfo.nodeTarget != null) {
                        val count = nodeBasedStarts.getOrDefault(queueInfo.nodeTarget, 0)
                        nodeBasedStarts[queueInfo.nodeTarget!!] = count + 1
                    } else {
                        val count = unassigned.getOrDefault(t.uniqueId, 0)
                        unassigned[t.uniqueId] = count + 1
                    }
                }
            }

            val minGlobal = template.minStartedServices
            val minPerNode = template.minStartedServicesPerNode

            val sum = nodeBasedStarts.values.sum() + unassigned.values.sum()
            if (sum < minGlobal && minGlobal > 0) {
                val needToStart = minGlobal - sum
                for (i in 0 until needToStart) {
                    serverFactory.queueStart(template)
                }
                return@forEach
            }
            if (unassigned.isEmpty() && minPerNode > 0) {
                if (nodeBasedStarts.values.none { it < minPerNode }) return@forEach
                nodeBasedStarts.forEach { (nodeId, count) ->
                    if (count >= minPerNode) return@forEach
                    val needStartToStart = minPerNode - count
                    for (i in 0 until needStartToStart) {
                        val info = ServerQueueInformation(
                            UUID.randomUUID(),
                            template,
                            null,
                            queueTime = System.currentTimeMillis(),
                            nodeTarget = nodeId
                        )
                        serverFactory.queueStart(info)
                    }
                }
            }
        }

        return false
    }

}