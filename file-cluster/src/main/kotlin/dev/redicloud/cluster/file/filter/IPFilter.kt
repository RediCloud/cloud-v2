package dev.redicloud.cluster.file.filter

import dev.redicloud.api.events.internal.node.NodeSuspendedEvent
import dev.redicloud.api.events.internal.node.file.FileNodeConnectedEvent
import dev.redicloud.api.events.internal.node.file.FileNodeDisconnectedEvent
import dev.redicloud.api.events.listen
import dev.redicloud.cluster.file.FileNodeRepository
import dev.redicloud.event.EventManager
import kotlinx.coroutines.runBlocking

class IPFilter(
    val eventManager: EventManager,
    val fileNodeRepository: FileNodeRepository
) {

    private val allowedIpCache = mutableListOf<String>()

    init {
        eventManager.listen<FileNodeConnectedEvent> {
            val fileNode = fileNodeRepository.getFileNode(it.serviceId) ?: return@listen
            allowedIpCache.add(fileNode.hostname)
        }
        eventManager.listen<FileNodeDisconnectedEvent> {
            val fileNode = fileNodeRepository.getFileNode(it.serviceId) ?: return@listen
            allowedIpCache.remove(fileNode.hostname)
        }
        eventManager.listen<NodeSuspendedEvent> {
            val fileNode = fileNodeRepository.getFileNode(it.serviceId) ?: return@listen
            allowedIpCache.remove(fileNode.hostname)
        }
        runBlocking {
            System.getProperty("redicloud.filter.ip.bypass", "127.0.0.1;0.0.0.0").split(";").forEach {
                allowedIpCache.add(it)
            }
            fileNodeRepository.getConnectedFileNodes().forEach { allowedIpCache.add(it.hostname) }
        }
    }

    fun canConnect(remoteAddress: String): Boolean = allowedIpCache.contains(remoteAddress)
}
