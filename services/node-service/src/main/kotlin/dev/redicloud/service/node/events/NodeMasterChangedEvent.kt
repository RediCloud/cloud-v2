package dev.redicloud.service.node.events

import dev.redicloud.event.EventFireType
import dev.redicloud.repository.node.CloudNode

class NodeMasterChangedEvent(node: CloudNode, val oldMaster: CloudNode?) : NodeEvent(node, EventFireType.GLOBAL)