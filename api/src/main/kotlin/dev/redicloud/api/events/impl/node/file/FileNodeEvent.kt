package dev.redicloud.api.events.impl.node.file

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.EventFireType
import dev.redicloud.utils.service.ServiceId

abstract class FileNodeEvent(val serviceId: ServiceId) : CloudEvent(EventFireType.NODE)