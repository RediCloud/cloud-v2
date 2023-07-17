package dev.redicloud.api.events.impl.server

import dev.redicloud.api.events.EventFireType
import dev.redicloud.utils.service.ServiceId

class CloudServerDisconnectedEvent(serviceId: ServiceId) : CloudServerEvent(serviceId, EventFireType.GLOBAL)