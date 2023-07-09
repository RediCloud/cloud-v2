package dev.redicloud.server.factory

import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.template.configuration.ConfigurationTemplate
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import java.util.LinkedList
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

data class ServerQueueInformation(
    val uniqueId: UUID = UUID.randomUUID(),
    val configurationTemplate: ConfigurationTemplate?,
    val serviceId: ServiceId?,
    val failedStarts: FailedStarts = FailedStarts(),
    val nodeStartOrder: MutableList<ServiceId> = mutableListOf(),
    val queueTime: Long
)

class FailedStarts : LinkedList<String>() {
    fun addFailedStart(serviceId: ServiceId, result: StartResultType) {
        val key = "${serviceId.toName()}:$result:"
        val current = firstOrNull { it.startsWith(key) }?.split(":")?.get(2)?.toIntOrNull() ?: 0
        removeIf { it.startsWith(key) }
        add("$key${current + 1}")
    }
    fun getFailedStarts(serviceId: ServiceId): Int {
        return filter { it.startsWith("${serviceId.toName()}:") }.sumOf { it.split(":")[2].toInt() }
    }
    fun getFailedStarts(serviceId: ServiceId, result: StartResultType): Int {
        return filter { it.startsWith("${serviceId.toName()}:$result:") }.sumOf { it.split(":")[2].toInt() }
    }
    fun getFailedNodes(): List<ServiceId> {
        return filter { it.split(":")[2].toIntOrNull() != 0 }.map { ServiceId.fromString(it.split(":")[0]) }
    }
    fun getFailedStats(): Int {
        var count = 0
        forEach { count += it.split(":")[2].toIntOrNull() ?: 0 }
        return count
    }
    fun toMap(): Map<ServiceId, Map<StartResultType, Int>> {
        val map = mutableMapOf<ServiceId, MutableMap<StartResultType, Int>>()
        forEach {
            val split = it.split(":")
            val serviceId = ServiceId.fromString(split[0])
            val result = StartResultType.valueOf(split[1])
            val count = split[2].toIntOrNull() ?: 0
            if (!map.containsKey(serviceId)) map[serviceId] = mutableMapOf()
            map[serviceId]!![result] = count
        }
        return map
    }
    fun removeFails(serviceId: ServiceId) {
        removeIf { it.startsWith("${serviceId.toName()}:") }
    }
}

fun ServerQueueInformation.isNextNode(serviceId: ServiceId): Boolean {
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

suspend fun ServerQueueInformation.calculateStartOrder(nodes: List<CloudNode>, serverRepository: ServerRepository): MutableList<ServiceId> {
    nodeStartOrder.clear()

    nodes.forEach {
        val priority = calculateStartPriority(it, serverRepository)
        if (priority < 0) return@forEach
        nodeStartOrder.add(it.serviceId)
    }

    return nodeStartOrder
}

suspend fun ServerQueueInformation.calculateStartPriority(cloudNode: CloudNode, serverRepository: ServerRepository): Int {
    var count = 0

    if (!cloudNode.isConnected()) {
        addFailedStart(cloudNode.serviceId, StartResultType.NODE_NOT_CONNECTED)
        addFailedNode(cloudNode.serviceId)
        return -1
    }

    if (configurationTemplate != null) {
        if (configurationTemplate.nodeIds.isNotEmpty() && !configurationTemplate.nodeIds.contains(cloudNode.serviceId)) {
            addFailedStart(cloudNode.serviceId, StartResultType.NODE_IS_NOT_ALLOWED)
            addFailedNode(cloudNode.serviceId)
            return -1
        }
    }else if(serviceId != null) {
        val server = serverRepository.getServer<CloudServer>(serviceId)
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
    }

    // Calculate memory usage in percent
    val memoryUsagePercent = cloudNode.currentMemoryUsage / cloudNode.maxMemory * 100
    count += memoryUsagePercent.toInt()

    return count
}