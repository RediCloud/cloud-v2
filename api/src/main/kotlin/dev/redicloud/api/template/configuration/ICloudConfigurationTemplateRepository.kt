package dev.redicloud.api.template.configuration

import java.util.UUID

interface ICloudConfigurationTemplateRepository {

    suspend fun getTemplate(uniqueId: UUID): ICloudConfigurationTemplate?

    suspend fun getTemplate(name: String): ICloudConfigurationTemplate?

    suspend fun existsTemplate(uniqueId: UUID): Boolean

    suspend fun existsTemplate(name: String): Boolean

    suspend fun createTemplate(configurationTemplate: ICloudConfigurationTemplate): ICloudConfigurationTemplate

    suspend fun updateTemplate(configurationTemplate: ICloudConfigurationTemplate): ICloudConfigurationTemplate

    suspend fun deleteTemplate(configurationTemplate: ICloudConfigurationTemplate): Boolean

    suspend fun getTemplates(): List<ICloudConfigurationTemplate>

}