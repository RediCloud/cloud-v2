package dev.redicloud.service.base.events.server

import dev.redicloud.event.CloudEvent
import dev.redicloud.event.EventFireType
import dev.redicloud.utils.service.ServiceId

abstract class CloudServerEvent(val serviceId: ServiceId, fireType: EventFireType) : CloudEvent(fireType)