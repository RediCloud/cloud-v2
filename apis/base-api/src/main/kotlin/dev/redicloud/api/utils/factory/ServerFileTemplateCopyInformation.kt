package dev.redicloud.api.utils.factory

import dev.redicloud.api.service.ServiceId
import java.util.UUID

data class ServerFileTemplateCopyInformation(
    val serviceId: ServiceId,
    val templateId: UUID,
    val path: String
)