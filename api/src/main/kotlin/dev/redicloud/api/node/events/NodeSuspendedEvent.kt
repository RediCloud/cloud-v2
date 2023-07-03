package dev.redicloud.service.base.events.node

import dev.redicloud.event.EventFireType
import dev.redicloud.utils.service.ServiceId

class NodeSuspendedEvent(serviceId: ServiceId, val suspender: ServiceId) : NodeEvent(serviceId, EventFireType.GLOBAL)