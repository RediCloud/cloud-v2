package dev.redicloud.service.base.events

import dev.redicloud.event.EventFireType
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.service.ServiceSession

class NodeConnectEvent(node: CloudNode) : NodeEvent(node, EventFireType.GLOBAL) {

    fun getSession(): ServiceSession = node.currentSession()!!

}