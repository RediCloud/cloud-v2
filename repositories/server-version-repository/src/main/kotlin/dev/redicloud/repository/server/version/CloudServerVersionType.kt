package dev.redicloud.repository.server.version

import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.*
import java.io.File
import java.net.URL
import java.util.*


class CloudServerVersionType(
    val uniqueId: UUID = UUID.randomUUID(),
    var name: String,
    var versionHandlerName: String,
    var proxy: Boolean,
    val defaultType: Boolean = false,
    var connectorPluginName: String,
    var connectorDownloadUrl: String?,
    var connectorFolder: String,
    var libPattern: String? = null,
    jvmArguments: MutableList<String> = mutableListOf(),
    environmentVariables: MutableMap<String, String> = mutableMapOf(),
    programmParameters: MutableList<String> = mutableListOf(),
    defaultFiles: MutableMap<String, String> = mutableMapOf(),
    fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
) : ProcessConfiguration(
    jvmArguments,
    environmentVariables,
    programmParameters,
    defaultFiles,
    fileEdits
) {

    companion object {
        private val logger = LogManager.Companion.logger(CloudServerVersionType::class)
    }

    fun getConnectorFile(nodeFolder: Boolean): File {
        return if (nodeFolder) {
            File(CONNECTORS_FOLDER.getFile(), connectorPluginName
                .replace("%cloud_version%", CLOUD_VERSION)
                .replace("%build_number%", BUILD_NUMBER)
            )
        }else {
            File(connectorFolder, connectorPluginName
                .replace("%cloud_version%", CLOUD_VERSION)
                .replace("%build_number%", BUILD_NUMBER)
            )
        }
    }

    fun getConnectorURL(): URL {
        return URL(connectorDownloadUrl
            ?.replace("%cloud_version%", CLOUD_VERSION)
            ?.replace("%build_number%", BUILD_NUMBER)
            ?: throw IllegalStateException("Connector download url is null!")
        )
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

    override fun equals(other: Any?): Boolean {
        if (other !is CloudServerVersionType) return false

        if (uniqueId != other.uniqueId) return false
        if (name != other.name) return false
        if (versionHandlerName != other.versionHandlerName) return false
        if (proxy != other.proxy) return false
        if (defaultType != other.defaultType) return false
        if (connectorPluginName != other.connectorPluginName) return false
        if (connectorDownloadUrl != other.connectorDownloadUrl) return false
        if (connectorFolder != other.connectorFolder) return false
        return libPattern == other.libPattern
    }

}