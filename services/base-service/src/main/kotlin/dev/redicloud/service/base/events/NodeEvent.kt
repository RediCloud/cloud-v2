package dev.redicloud.service.base.events

import dev.redicloud.event.CloudEvent
import dev.redicloud.event.EventFireType
import dev.redicloud.utils.service.ServiceId

abstract class NodeEvent(
    val serviceId: ServiceId,
    fireType: EventFireType
) : CloudEvent(fireType)