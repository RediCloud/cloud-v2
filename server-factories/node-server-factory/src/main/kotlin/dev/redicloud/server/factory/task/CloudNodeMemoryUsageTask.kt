package dev.redicloud.server.factory.task

import dev.redicloud.api.service.ServiceId
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask

class CloudNodeMemoryUsageTask(
    private val serverFactory: ServerFactory,
    private val nodeRepository: NodeRepository,
    private val hostingId: ServiceId
) : CloudTask() {

    private var lastUpdate = -1L

    override suspend fun execute(): Boolean {
        if (lastUpdate + 1500 > System.currentTimeMillis()) return false
        lastUpdate = System.currentTimeMillis()
        val hostedProcesses = serverFactory.hostedProcesses.toList()
        val memoryUsage = hostedProcesses.sumOf { it.configurationTemplate.maxMemory }
        val thisNode = nodeRepository.getNode(hostingId)!!
        thisNode.currentMemoryUsage = memoryUsage
        nodeRepository.updateNode(thisNode)
        return false
    }


}