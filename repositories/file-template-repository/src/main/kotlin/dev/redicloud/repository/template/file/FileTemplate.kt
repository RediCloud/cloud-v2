package dev.redicloud.repository.template.file

import dev.redicloud.utils.TEMPLATE_FOLDER
import java.io.File
import java.util.UUID

data class FileTemplate(
    val uniqueId: UUID = UUID.randomUUID(),
    var name: String,
    var prefix: String,
    val inherited: MutableList<String> = mutableListOf()
) {

    fun getFolder(): File {
        val parent = File(TEMPLATE_FOLDER.getFile().absolutePath, prefix)
        return File(name, parent.absolutePath)
    }

}