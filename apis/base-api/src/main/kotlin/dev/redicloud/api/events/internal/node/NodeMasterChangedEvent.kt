package dev.redicloud.api.events.internal.node

import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

class NodeMasterChangedEvent(serviceId: ServiceId, val oldMaster: ServiceId?) : NodeEvent(serviceId, EventFireType.GLOBAL)