package dev.redicloud.api.events.impl.node

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.EventFireType
import dev.redicloud.utils.service.ServiceId

abstract class NodeEvent(
    val serviceId: ServiceId,
    fireType: EventFireType
) : CloudEvent(fireType)