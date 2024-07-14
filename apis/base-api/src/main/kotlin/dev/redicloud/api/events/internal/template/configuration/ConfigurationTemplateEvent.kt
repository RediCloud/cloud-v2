package dev.redicloud.api.events.internal.template.configuration

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.EventFireType
import java.util.UUID

abstract class ConfigurationTemplateEvent(
    val configurationTemplateId: UUID,
    fireType: EventFireType
) : CloudEvent(fireType)