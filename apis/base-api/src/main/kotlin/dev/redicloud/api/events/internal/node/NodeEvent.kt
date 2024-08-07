package dev.redicloud.api.events.internal.node

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

abstract class NodeEvent(
    val serviceId: ServiceId,
    fireType: EventFireType
) : CloudEvent(fireType)