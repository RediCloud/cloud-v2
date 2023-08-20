package dev.redicloud.api.events.impl.template.configuration

import dev.redicloud.api.events.EventFireType
import java.util.UUID

class ConfigurationTemplateUpdateEvent(
    configurationTemplateId: UUID
) : ConfigurationTemplateEvent(configurationTemplateId, EventFireType.GLOBAL)