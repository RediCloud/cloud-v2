package dev.redicloud.api.events.impl.node

import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

class NodeSuspendedEvent(serviceId: ServiceId, val suspender: ServiceId) : NodeEvent(serviceId, EventFireType.GLOBAL)