package dev.redicloud.service.base.events

import dev.redicloud.event.EventFireType
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.service.ServiceSession

class NodeDisconnectEvent(node: CloudNode) : NodeEvent(node, EventFireType.GLOBAL) {

    fun getLastSession(): ServiceSession = node.getSessions().last()

}