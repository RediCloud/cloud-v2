package dev.redicloud.api.events.impl.module

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.modules.IModuleHandler

class ModuleHandlerInitializedEvent(
    val moduleHandler: IModuleHandler
) : CloudEvent(EventFireType.LOCAL)