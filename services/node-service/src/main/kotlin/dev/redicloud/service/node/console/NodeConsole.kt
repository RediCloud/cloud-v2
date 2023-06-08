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
        writeLine("Node ${it.node.getIdentifyingName()} was suspended by ${it.suspender.getIdentifyingName()}!")
    }

    private val onNodeConnect = eventManager.listen<NodeConnectEvent> {
        if (it.node.serviceId == nodeConfiguration.toServiceId()) return@listen
        writeLine("Node %hc%${it.node.getIdentifyingName()} connected to the cluster!")
    }

    private val onNodeDisconnect = eventManager.listen<NodeDisconnectEvent> {
        if (it.node.serviceId == nodeConfiguration.toServiceId()) return@listen
        writeLine("Node ${it.node.getIdentifyingName()} disconnected from the cluster!")
    }

    private val onNodeMasterChange = eventManager.listen<NodeMasterChangedEvent> {
        writeLine("Node ${it.node.getIdentifyingName()} is now the master node!")
    }

}