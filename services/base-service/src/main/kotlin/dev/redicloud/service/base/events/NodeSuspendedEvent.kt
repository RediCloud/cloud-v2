package dev.redicloud.service.base.events

import dev.redicloud.event.EventFireType
import dev.redicloud.repository.node.CloudNode

class NodeSuspendedEvent(node: CloudNode, val suspender: CloudNode) : NodeEvent(node, EventFireType.GLOBAL)