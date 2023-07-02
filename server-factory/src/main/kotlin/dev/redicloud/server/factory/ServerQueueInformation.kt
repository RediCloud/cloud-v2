package dev.redicloud.server.factory

import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.utils.service.ServiceId
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

data class ServerQueueInformation(
    val uniqueId: UUID = UUID.randomUUID(),
    val configurationTemplate: ConfigurationTemplate,
    val failedStarts: MutableMap<ServiceId, MutableMap<StartResultType, Int>> = mutableMapOf(),
    val nodeStartOrder: MutableList<ServiceId> = mutableListOf(),
    val queueTime: Long
)

fun ServerQueueInformation.isNextNode(serviceId: ServiceId): Boolean {
    return nodeStartOrder.isNotEmpty() && nodeStartOrder[0] == serviceId
}

fun ServerQueueInformation.addFailedStart(
    serviceId: ServiceId,
    result: StartResultType
) {
    val subMap = failedStarts.getOrDefault(serviceId, mutableMapOf())
    subMap[result] = subMap.getOrDefault(result, 0) + 1
    failedStarts[serviceId] = subMap
}

fun ServerQueueInformation.addFailedNode(
    serviceId: ServiceId
) {
    nodeStartOrder.remove(serviceId)
}

fun ServerQueueInformation.getFailedStarts(serviceId: ServiceId): Int {
    return failedStarts.getOrDefault(serviceId, mutableMapOf()).values.sum()
}

fun ServerQueueInformation.getFailedStarts(serviceId: ServiceId, result: StartResultType): Int {
    return failedStarts.getOrDefault(serviceId, mutableMapOf()).getOrDefault(result, 0)
}

fun ServerQueueInformation.getFailedStarts(): Int {
    return failedStarts.values.sumBy { it.values.sum() }
}

fun ServerQueueInformation.calculateStartOrder(nodes: List<CloudNode>): MutableList<ServiceId> {
    nodeStartOrder.clear()

    nodes.forEach {
        val priority = calculateStartPriority(it)
        if (priority < 0) return@forEach
        nodeStartOrder.add(it.serviceId)
    }

    return nodeStartOrder
}

fun ServerQueueInformation.calculateStartPriority(cloudNode: CloudNode): Int {
    var count = 0

    if (!cloudNode.isConnected()) {
        addFailedStart(cloudNode.serviceId, StartResultType.NODE_NOT_CONNECTED)
        return -1
    }

    if (configurationTemplate.nodeIds.isNotEmpty() && !configurationTemplate.nodeIds.contains(cloudNode.serviceId)) {
        addFailedStart(cloudNode.serviceId, StartResultType.NODE_IS_NOT_ALLOWED)
        return -1
    }

    if (cloudNode.currentMemoryUsage + configurationTemplate.maxMemory > cloudNode.maxMemory) {
        addFailedStart(cloudNode.serviceId, StartResultType.RAM_USAGE_TOO_HIGH)
        return -1
    }

    // Calculate memory usage in percent
    val memoryUsagePercent = cloudNode.currentMemoryUsage / cloudNode.maxMemory * 100
    count += memoryUsagePercent.toInt()

    return count
}