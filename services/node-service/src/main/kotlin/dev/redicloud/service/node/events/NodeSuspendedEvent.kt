package dev.redicloud.service.node.events

import dev.redicloud.event.EventFireType
import dev.redicloud.repository.node.CloudNode

class NodeSuspendedEvent(node: CloudNode, val suspender: CloudNode) : NodeEvent(node, EventFireType.GLOBAL)