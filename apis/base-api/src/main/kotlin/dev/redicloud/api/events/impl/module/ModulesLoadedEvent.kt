package dev.redicloud.api.events.impl.module

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.modules.CloudModule

data class ModuleLifeCycleChangedEvent(val module: CloudModule) : CloudEvent(EventFireType.LOCAL)