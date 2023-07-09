package dev.redicloud.api.server.events.server

import dev.redicloud.event.EventFireType
import dev.redicloud.service.base.events.server.CloudServerEvent
import dev.redicloud.utils.service.ServiceId

class CloudServerDeleteEvent(serviceId: ServiceId, val name: String) : CloudServerEvent(serviceId, EventFireType.GLOBAL)