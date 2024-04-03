package dev.redicloud.module.rest.fetcher

import dev.redicloud.api.template.configuration.ICloudConfigurationTemplate
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplateRepository
import java.util.*

class ConfigurationTemplateFetcher(
    private val configurationTemplateRepository: ICloudConfigurationTemplateRepository
) {

    suspend fun fetchConfigurationTemplateByName(name: String?): ICloudConfigurationTemplate? {
        if (name == null) {
            return null
        }
        return configurationTemplateRepository.getTemplate(name)
    }

    suspend fun fetchConfigurationTemplateById(id: String?): ICloudConfigurationTemplate? {
        return try {
            val uuid = UUID.fromString(id)
            configurationTemplateRepository.getTemplate(uuid)
        } catch (e: IllegalArgumentException) {
            return null
        }
    }

}