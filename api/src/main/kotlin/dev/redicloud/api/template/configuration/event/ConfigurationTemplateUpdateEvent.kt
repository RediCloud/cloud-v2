package dev.redicloud.api.template.configuration.event

import dev.redicloud.event.EventFireType
import java.util.UUID

class ConfigurationTemplateUpdateEvent(
    configurationTemplateId: UUID
) : ConfigurationTemplateEvent(configurationTemplateId, EventFireType.GLOBAL)