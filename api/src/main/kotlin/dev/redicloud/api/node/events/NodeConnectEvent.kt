package dev.redicloud.service.base.events.node

import dev.redicloud.event.EventFireType
import dev.redicloud.utils.service.ServiceId

class NodeConnectEvent(serviceId: ServiceId) : NodeEvent(serviceId, EventFireType.GLOBAL)