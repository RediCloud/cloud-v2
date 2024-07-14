package dev.redicloud.api.events.internal.server

import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

class CloudServerDeleteEvent(serviceId: ServiceId, val name: String) : CloudServerEvent(serviceId, EventFireType.GLOBAL)