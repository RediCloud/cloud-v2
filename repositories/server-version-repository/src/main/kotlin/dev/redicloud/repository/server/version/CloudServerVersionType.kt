package dev.redicloud.repository.server.version

import dev.redicloud.utils.CLOUD_VERSION
import dev.redicloud.utils.ConfigurationFileEditor
import java.io.File
import java.util.*


data class CloudServerVersionType(
    val uniqueId: UUID = UUID.randomUUID(),
    var name: String,
    var versionHandlerName: String,
    var proxy: Boolean,
    val jvmArguments: MutableList<String> = mutableListOf(),
    val programmArguments: MutableList<String> = mutableListOf(),
    // key = file, pair first = key, pair second = value
    val fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf(),
    val defaultType: Boolean = false,
    var connectorPluginName: String,
    var connectorDownloadUrl: String?,
    var connectorFolder: String
) {

    fun getConnectorFile(): File {
        return File(connectorFolder, connectorPluginName.replace("%cloud_version%", CLOUD_VERSION))
    }

    fun isUnknown(): Boolean = name.lowercase() == "unknown"

    fun doFileEdits(folder: File, action: (String) -> String = { it }) {
        fileEdits.forEach { (file, editInfo) ->
            val fileToEdit = File(folder, file)
            if (!fileToEdit.exists()) return
            val editor = ConfigurationFileEditor.ofFile(fileToEdit) ?: return
            editInfo.forEach { key, value ->
                editor.setValue(key, action(value))
            }
            editor.saveToFile(fileToEdit)
        }
    }

}