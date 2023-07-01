package dev.redicloud.repository.template.file

import dev.redicloud.utils.TEMPLATE_FOLDER
import java.io.File
import java.util.*

data class FileTemplate(
    val uniqueId: UUID = UUID.randomUUID(),
    var prefix: String,
    var name: String,
    val inherited: MutableList<UUID> = mutableListOf()
) {

    fun getDisplayName(): String {
        return "$name/$prefix"
    }

    fun getFolder(): File {
        val parent = File(TEMPLATE_FOLDER.getFile().absolutePath, prefix)
        return File(name, parent.absolutePath)
    }

}