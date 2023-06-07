package dev.redicloud.service.node.events

import dev.redicloud.event.EventFireType
import dev.redicloud.repository.node.CloudNode

class NodeSuspendEvent(node: CloudNode) : NodeEvent(node, EventFireType.GLOBAL)