package dev.redicloud.cluster.file.event

import dev.redicloud.event.CloudEvent
import dev.redicloud.event.EventFireType
import dev.redicloud.utils.service.ServiceId

abstract class FileNodeEvent(val serviceId: ServiceId) : CloudEvent(EventFireType.NODE)