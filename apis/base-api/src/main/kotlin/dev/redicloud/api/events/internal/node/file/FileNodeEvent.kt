package dev.redicloud.api.events.internal.node.file

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

abstract class FileNodeEvent(val serviceId: ServiceId) : CloudEvent(EventFireType.NODE)