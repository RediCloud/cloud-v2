package dev.redicloud.api.events.impl.server

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.EventFireType
import dev.redicloud.utils.service.ServiceId

abstract class CloudServerEvent(val serviceId: ServiceId, fireType: EventFireType) : CloudEvent(fireType)