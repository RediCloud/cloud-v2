package dev.redicloud.repository.template.file

import dev.redicloud.utils.TEMPLATE_FOLDER
import java.io.File

data class FileTemplate(val name: String, val prefix: String, val inherited: MutableList<String>) {

    fun getFolder(): File {
        val parent = File(prefix, TEMPLATE_FOLDER.getFile().absolutePath)
        return File(name, parent.absolutePath)
    }

}