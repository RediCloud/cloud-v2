package dev.redicloud.repository.server.version

import dev.redicloud.utils.ConfigurationFileEditor
import khttp.get
import java.io.File
import java.util.*


data class CloudServerVersionType(
    val uniqueId: UUID = UUID.randomUUID(),
    val name: String,
    val versionHandlerName: String,
    val craftBukkitBased: Boolean,
    val proxy: Boolean,
    val jvmArguments: List<String> = mutableListOf(),
    val programmArguments: List<String> = mutableListOf(),
    // key = file, pair first = key, pair second = value
    val fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf(),
    val defaultType: Boolean = false
) {

    fun isUnknown(): Boolean = name.lowercase() == "unknown"

    fun doFileEdits(folder: File) {
        fileEdits.forEach { (file, editInfo) ->
            val fileToEdit = File(folder, file)
            if (!fileToEdit.exists()) return
            val editor = ConfigurationFileEditor.ofFile(fileToEdit) ?: return
            editInfo.forEach { key, value ->
                editor.setValue(key, value)
            }
            editor.saveToFile(fileToEdit)
        }
    }

}