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
        return "$prefix/$name"
    }

    fun getFolder(): File {
        return File(getPrefixFolder().absolutePath, name)
    }

    fun getPrefixFolder(): File {
        return File(TEMPLATE_FOLDER.getFile().absolutePath, prefix)
    }

}