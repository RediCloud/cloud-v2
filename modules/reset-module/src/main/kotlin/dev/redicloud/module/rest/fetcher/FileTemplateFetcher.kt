package dev.redicloud.module.rest.fetcher

import dev.redicloud.api.template.file.ICloudFileTemplate
import dev.redicloud.api.template.file.ICloudFileTemplateRepository
import java.util.*

class FileTemplateFetcher(
    private val fileTemplateRepository: ICloudFileTemplateRepository
) {

    suspend fun fetchFileTemplateByDisplayName(displayName: String?): ICloudFileTemplate? {
        if (displayName == null) {
            return null
        }
        return fileTemplateRepository.getTemplate(displayName)
    }

    suspend fun fetchFileTemplateById(id: String?): ICloudFileTemplate? {
        return try {
            val uuid = UUID.fromString(id)
            fileTemplateRepository.getTemplate(uuid)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

}