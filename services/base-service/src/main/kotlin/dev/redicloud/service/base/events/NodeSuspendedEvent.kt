package dev.redicloud.service.base.events

import dev.redicloud.event.EventFireType
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.utils.service.ServiceId

class NodeSuspendedEvent(serviceId: ServiceId, val suspender: CloudNode) : NodeEvent(serviceId, EventFireType.GLOBAL)