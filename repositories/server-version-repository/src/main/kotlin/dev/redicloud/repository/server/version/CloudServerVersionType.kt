package dev.redicloud.repository.server.version

import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.logging.LogManager
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
    val defaultFiles: MutableMap<String, String> = mutableMapOf(),
    val defaultType: Boolean = false,
    var connectorPluginName: String,
    var connectorDownloadUrl: String?,
    var connectorFolder: String,
    var libPattern: String?
) {

    companion object {
        private val logger = LogManager.Companion.logger(CloudServerVersionType::class)
    }

    fun getConnectorFile(): File {
        return File(connectorFolder, connectorPluginName.replace("%cloud_version%", CLOUD_VERSION))
    }

    fun isUnknown(): Boolean = name.lowercase() == "unknown"

    fun doFileEdits(folder: File, action: (String) -> String = { it }) {
        fileEdits.forEach { (file, editInfo) ->
            val fileToEdit = File(folder, file)
            if (!fileToEdit.exists()) {
                logger.warning("File $fileToEdit does not exist! So it can not be edited!")
                return@forEach
            }
            val editor = ConfigurationFileEditor.ofFile(fileToEdit)
            if (editor == null) {
                logger.warning("§cFile ${toConsoleValue(fileToEdit, false)} is not a configuration file! So it can not be edited!")
                return@forEach
            }
            editInfo.forEach { (key, value) ->
                try {
                    editor.setValue(key, action(value))
                }catch (e: IllegalStateException) {
                    logger.warning("§cKey ${toConsoleValue(key, false)} does not exist in file ${toConsoleValue(fileToEdit, false)}!")
                }
            }
            editor.saveToFile(fileToEdit)
        }
    }

}