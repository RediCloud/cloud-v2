package dev.redicloud.service.node.console

import dev.redicloud.console.Console
import dev.redicloud.event.EventManager
import dev.redicloud.service.node.NodeConfiguration
import dev.redicloud.service.node.events.NodeConnectEvent
import dev.redicloud.service.node.events.NodeDisconnectEvent
import dev.redicloud.service.node.events.NodeMasterChangedEvent
import dev.redicloud.service.node.events.NodeSuspendedEvent

class NodeConsole(nodeConfiguration: NodeConfiguration, eventManager: EventManager) :
    Console(nodeConfiguration.nodeName, eventManager, true) {

    private val onSuspendNode = eventManager.listen<NodeSuspendedEvent> {
        writeLine("${it.node.getIdentifyingName()}§8: §4● §8(%tc%suspended by ${it.suspender.getIdentifyingName(false)}§8)")
    }

    private val onNodeConnect = eventManager.listen<NodeConnectEvent> {
        if (it.node.serviceId == nodeConfiguration.toServiceId()) return@listen
        writeLine("${it.node.getIdentifyingName()}§8: §2● §8(%tc%connected to the cluster§8)")
    }

    private val onNodeDisconnect = eventManager.listen<NodeDisconnectEvent> {
        if (it.node.serviceId == nodeConfiguration.toServiceId()) return@listen
        writeLine("${it.node.getIdentifyingName()}§8: §c● §8(%tc%disconnected from the cluster§8)")
    }

    private val onNodeMasterChange = eventManager.listen<NodeMasterChangedEvent> {
        writeLine("${it.node.getIdentifyingName()}§8: §6● §8(%tc%new master§8)")
    }

    init {
        this.sendHeader()
    }

    override fun handleUserInterrupt(e: Exception) {
        commandManager.getCommand("exit")!!.getSubCommand("")!!.execute(commandManager.actor, emptyList())
    }

}