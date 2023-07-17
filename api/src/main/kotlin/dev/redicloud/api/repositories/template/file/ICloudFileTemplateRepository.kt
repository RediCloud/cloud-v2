package dev.redicloud.api.repositories.template.file

import dev.redicloud.utils.service.ServiceId
import java.util.UUID

interface ICloudFileTemplateRepository {

    suspend fun getTemplate(uniqueId: UUID): ICloudFileTemplate?

    suspend fun getTemplate(displayName: String): ICloudFileTemplate?

    suspend fun getTemplate(name: String, prefix: String): ICloudFileTemplate?

    suspend fun existsTemplate(uniqueId: UUID, prefix: String): Boolean

    suspend fun existsTemplate(displayName: String): Boolean

    suspend fun existsTemplate(name: String, prefix: String): Boolean

    suspend fun deleteTemplate(uniqueId: UUID): Boolean

    suspend fun updateTemplate(template: ICloudFileTemplate): ICloudFileTemplate

    suspend fun createTemplate(template: ICloudFileTemplate): ICloudFileTemplate

    suspend fun getTemplates(): List<ICloudFileTemplate>

    suspend fun collectTemplates(vararg templates: ICloudFileTemplate): List<ICloudFileTemplate>

    suspend fun pushTemplates(serviceId: ServiceId)

}