package dev.redicloud.service.node.events

import dev.redicloud.event.EventFireType
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.service.ServiceSession

class NodeDisconnectEvent(node: CloudNode) : NodeEvent(node, EventFireType.GLOBAL) {

    fun getLastSession(): ServiceSession = node.getSessions().last()

}