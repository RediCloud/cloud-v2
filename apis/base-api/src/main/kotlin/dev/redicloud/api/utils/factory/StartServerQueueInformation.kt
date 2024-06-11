package dev.redicloud.api.utils.factory

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.node.ICloudNode
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplate
import java.util.LinkedList
import java.util.UUID

data class ServerQueueInformation(
    val uniqueId: UUID = UUID.randomUUID(), // Random unique id to identify
    val configurationTemplate: ICloudConfigurationTemplate?, // The configuration template
    val serviceId: ServiceId?, // The static server id
    val failedStarts: FailedStarts = FailedStarts(), // Failed starts with the failed node and the reason
    val nodeStartOrder: MutableList<ServiceId> = mutableListOf(), // The possible start nodes in order
    val nodeTarget: ServiceId? = null, // The current targeted node
    val queueTime: Long // The time when the queue entry was created
)

class FailedStarts : LinkedList<String>() {
    fun addFailedStart(serviceId: ServiceId, result: StartResultType) {
        val key = "${serviceId.toName()}:${result.name}:"
        val current = firstOrNull { it.startsWith(key) }?.split(":")?.get(2)?.toIntOrNull() ?: 0
        removeIf { it.startsWith(key) }
        add("$key${current + 1}")
    }
    fun getFailedStarts(serviceId: ServiceId): Int {
        return filter { it.startsWith("${serviceId.toName()}:") }.sumOf { it.split(":")[2].toInt() }
    }
    fun getFailedStarts(serviceId: ServiceId, result: StartResultType): Int {
        return filter { it.startsWith("${serviceId.toName()}:${result.name}:") }.sumOf { it.split(":")[2].toInt() }
    }
    fun getFailedNodes(): List<ServiceId> {
        return filter { it.split(":")[2].toIntOrNull() != 0 }.map { ServiceId.fromString(it.split(":")[0]) }
    }
    fun getFailedStats(): Int {
        var count = 0
        forEach { count += it.split(":")[2].toIntOrNull() ?: 0 }
        return count
    }
    fun removeFails(serviceId: ServiceId) {
        removeIf { it.startsWith("${serviceId.toName()}:") }
    }
}

fun ServerQueueInformation.isNextNode(serviceId: ServiceId): Boolean {
    if (nodeTarget != null && nodeTarget == serviceId) return true
    return nodeStartOrder.isNotEmpty() && nodeStartOrder[0] == serviceId
}

fun ServerQueueInformation.addFailedStart(
    serviceId: ServiceId,
    result: StartResultType
) {
    failedStarts.addFailedStart(serviceId, result)
}

fun ServerQueueInformation.addFailedNode(
    serviceId: ServiceId
) {
    nodeStartOrder.remove(serviceId)
}

fun ServerQueueInformation.getFailedStarts(serviceId: ServiceId): Int {
    return failedStarts.getFailedStarts(serviceId)
}

fun ServerQueueInformation.getFailedStarts(serviceId: ServiceId, result: StartResultType): Int {
    return failedStarts.getFailedStarts(serviceId, result)
}

fun ServerQueueInformation.getFailedStarts(): Int {
    return failedStarts.getFailedStats()
}

suspend fun ServerQueueInformation.calculateStartOrder(nodes: List<ICloudNode>, serverRepository: ICloudServerRepository): MutableList<ServiceId> {
    nodeStartOrder.clear()

    nodes.forEach {
        val priority = calculateStartPriority(it, serverRepository)
        if (priority < 0) return@forEach
        nodeStartOrder.add(it.serviceId)
    }

    return nodeStartOrder
}

suspend fun ServerQueueInformation.calculateStartPriority(cloudNode: ICloudNode, serverRepository: ICloudServerRepository): Int {
    var count = 0

    if (!cloudNode.connected) {
        addFailedStart(cloudNode.serviceId, StartResultType.NODE_NOT_CONNECTED)
        addFailedNode(cloudNode.serviceId)
        return -1
    }

    if(serviceId != null) {
        val server = serverRepository.getServer<ICloudServer>(serviceId)
        val storedConfigurationTemplate = server?.configurationTemplate
        if (storedConfigurationTemplate != null && storedConfigurationTemplate.nodeIds.isNotEmpty() && !storedConfigurationTemplate.nodeIds.contains(cloudNode.serviceId)) {
            addFailedStart(cloudNode.serviceId, StartResultType.NODE_IS_NOT_ALLOWED)
            addFailedNode(cloudNode.serviceId)
            return -1
        }
        if (server != null && server.configurationTemplate.static && server.hostNodeId != cloudNode.serviceId ) {
            addFailedStart(cloudNode.serviceId, StartResultType.NODE_IS_NOT_ALLOWED)
            addFailedNode(cloudNode.serviceId)
            return -1
        }
    }else if (configurationTemplate != null) {
        if (configurationTemplate.nodeIds.isNotEmpty() && !configurationTemplate.nodeIds.contains(cloudNode.serviceId)) {
            addFailedStart(cloudNode.serviceId, StartResultType.NODE_IS_NOT_ALLOWED)
            addFailedNode(cloudNode.serviceId)
            return -1
        }
    }

    // Calculate memory usage in percent
    val memoryUsagePercent = cloudNode.currentMemoryUsage / cloudNode.maxMemory * 100
    count += memoryUsagePercent.toInt()

    return count
}