package dev.redicloud.api.events.internal.server

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

abstract class CloudServerEvent(val serviceId: ServiceId, fireType: EventFireType) : CloudEvent(fireType)