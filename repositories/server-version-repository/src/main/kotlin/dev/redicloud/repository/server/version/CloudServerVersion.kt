package dev.redicloud.repository.server.version

import dev.redicloud.console.commands.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.ConfigurationFileEditor
import java.io.File
import java.util.UUID

class CloudServerVersion(
    val uniqueId: UUID,
    var typeId: UUID?,
    var projectName: String,
    var customDownloadUrl: String?,
    var libPattern: String?,
    var buildId: String?,
    var version: ServerVersion,
    var javaVersionId: UUID?,
    val defaultFiles: MutableMap<String, String>,
    val fileEdits: MutableMap<String, MutableMap<String, String>>
) {

    companion object {
        private val logger = LogManager.Companion.logger(CloudServerVersion::class)
    }


    fun getDisplayName(): String {
        return "${projectName}_${version.name}"
    }

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