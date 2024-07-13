package dev.redicloud.server.factory.task

import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplateRepository
import dev.redicloud.server.factory.ServerFactory

class CloudAutoStartServerTask(
    private val configurationTemplateRepository: ConfigurationTemplateRepository,
    private val serverRepository: ServerRepository,
    private val serverFactory: ServerFactory,
    private val nodeRepository: NodeRepository
) : AbstractCloudFactoryTask(serverFactory) {

    override suspend fun execute(): Boolean {

        if (serverFactory.shutdown) return true

        val templates = configurationTemplateRepository.getTemplates()
        val nodes = nodeRepository.getConnectedNodes()
        val registeredServers = serverRepository.getRegisteredServers()
        val unassignedQueuedServerStarts = getUnassignedQueuedServerStarts()

        val startedServers = getStartedServers(nodes, registeredServers, templates)
        val nodeServerStarts = getNodeServerStarts(startedServers)

        templates.forEach { template ->
            if (serverFactory.shutdown) return true

            val minGlobal = template.minStartedServices
            val sum = startedServers.getOrDefault(template, emptyMap()).values.sum() + unassignedQueuedServerStarts.values.sum()
            if (sum < minGlobal && minGlobal > 0) {
                queueServerStart(template, minGlobal - sum, registeredServers)
                return@forEach
            }

            nodeServerStarts.forEach { (nodeId, starts) ->
                starts.forEach { (template, amount) ->
                    queueServerStart(template, amount, registeredServers, nodeId)
                }
            }
        }

        return false
    }

}