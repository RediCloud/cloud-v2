package dev.redicloud.api.template.configuration.event

import dev.redicloud.event.CloudEvent
import dev.redicloud.event.EventFireType
import java.util.UUID

abstract class ConfigurationTemplateEvent(
    val configurationTemplateId: UUID,
    fireType: EventFireType
) : CloudEvent(fireType)