package dev.redicloud.repository.template.file

import dev.redicloud.api.template.file.ICloudFileTemplate
import dev.redicloud.cache.IClusterCacheObject
import dev.redicloud.api.utils.TEMPLATE_FOLDER
import java.io.File
import java.util.*

data class FileTemplate(
    override val uniqueId: UUID = UUID.randomUUID(),
    override var prefix: String,
    override var name: String,
    override val inherited: MutableList<UUID> = mutableListOf()
) : IClusterCacheObject, ICloudFileTemplate {

    override val displayName: String
        get() {
            return "$prefix-$name"
        }

    override val folder: File
        get() {
            return File(prefixFolder.absolutePath, name)
        }

    override val prefixFolder: File
        get() {
            return File(TEMPLATE_FOLDER.getFile().absolutePath, prefix)
        }

    fun copy(name: String): ICloudFileTemplate {
        return FileTemplate(
            uniqueId = UUID.randomUUID(),
            prefix = prefix,
            name = name,
            inherited = inherited
        )
    }

}