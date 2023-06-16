package dev.redicloud.service.node.console

import dev.redicloud.console.Console
import dev.redicloud.event.EventManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.service.node.NodeConfiguration
import dev.redicloud.service.base.events.NodeConnectEvent
import dev.redicloud.service.base.events.NodeDisconnectEvent
import dev.redicloud.service.base.events.NodeMasterChangedEvent
import dev.redicloud.service.base.events.NodeSuspendedEvent
import kotlinx.coroutines.runBlocking

class NodeConsole(nodeConfiguration: NodeConfiguration, eventManager: EventManager, nodeRepository: NodeRepository) :
    Console(nodeConfiguration.nodeName, eventManager, true) {

    private val onSuspendNode = eventManager.listen<NodeSuspendedEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            writeLine("${node.getIdentifyingName()}§8: §4● §8(%tc%suspended by ${it.suspender.getIdentifyingName(false)} because node is reachable§8)")
        }
    }

    private val onNodeConnect = eventManager.listen<NodeConnectEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            writeLine("${node.getIdentifyingName()}§8: §2● §8(%tc%connected to the cluster§8)")
        }
    }

    private val onNodeDisconnect = eventManager.listen<NodeDisconnectEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            writeLine("${node.getIdentifyingName()}§8: §c● §8(%tc%disconnected from the cluster§8)")
        }
    }

    private val onNodeMasterChange = eventManager.listen<NodeMasterChangedEvent> {
        runBlocking {
            val node = nodeRepository.getNode(it.serviceId) ?: return@runBlocking
            writeLine("${node.getIdentifyingName()}§8: §6● §8(%tc%new master§8)")
        }
    }

    init {
        this.sendHeader()
    }

    override fun handleUserInterrupt(e: Exception) {
        commandManager.getCommand("exit")!!.getSubCommand("")!!.execute(commandManager.actor, emptyList())
    }

}