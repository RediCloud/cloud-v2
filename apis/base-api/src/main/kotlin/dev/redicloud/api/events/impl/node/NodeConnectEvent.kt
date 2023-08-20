package dev.redicloud.api.events.impl.node

import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

class NodeConnectEvent(serviceId: ServiceId) : NodeEvent(serviceId, EventFireType.GLOBAL)