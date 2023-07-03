package dev.redicloud.service.base.events.server

import dev.redicloud.event.EventFireType
import dev.redicloud.utils.service.ServiceId

class CloudServerDisconnectedEvent(serviceId: ServiceId) : CloudServerEvent(serviceId, EventFireType.GLOBAL)