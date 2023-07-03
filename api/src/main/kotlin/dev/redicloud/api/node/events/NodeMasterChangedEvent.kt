package dev.redicloud.service.base.events.node

import dev.redicloud.event.EventFireType
import dev.redicloud.utils.service.ServiceId

class NodeMasterChangedEvent(serviceId: ServiceId, val oldMaster: ServiceId?) : NodeEvent(serviceId, EventFireType.GLOBAL)