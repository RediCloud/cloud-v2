package dev.redicloud.service.base.events

import dev.redicloud.event.EventFireType
import dev.redicloud.repository.node.CloudNode
import dev.redicloud.repository.service.ServiceSession
import dev.redicloud.utils.service.ServiceId

class NodeDisconnectEvent(serviceId: ServiceId) : NodeEvent(serviceId, EventFireType.GLOBAL)