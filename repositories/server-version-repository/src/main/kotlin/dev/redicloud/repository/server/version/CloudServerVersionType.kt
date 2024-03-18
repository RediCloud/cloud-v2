package dev.redicloud.repository.server.version

import dev.redicloud.api.version.ICloudServerVersionType
import dev.redicloud.api.utils.CONNECTORS_FOLDER
import dev.redicloud.cache.IClusterCacheObject
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.*
import java.io.File
import java.net.URL
import java.util.*


class CloudServerVersionType(
    override val uniqueId: UUID = UUID.randomUUID(),
    override var name: String,
    override var versionHandlerName: String,
    override var proxy: Boolean,
    override val defaultType: Boolean = false,
    override var connectorPluginName: String,
    override var connectorDownloadUrl: String?,
    override var connectorFolder: String,
    override var libPattern: String? = null,
    override val jvmArguments: MutableList<String> = mutableListOf(),
    override val environmentVariables: MutableMap<String, String> = mutableMapOf(),
    override val programParameters: MutableList<String> = mutableListOf(),
    override val defaultFiles: MutableMap<String, String> = mutableMapOf(),
    override val fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
) : IClusterCacheObject, ICloudServerVersionType {

    companion object {
        private val logger = LogManager.Companion.logger(CloudServerVersionType::class)
    }

    override fun getParsedConnectorFile(nodeFolder: Boolean): File {
        return if (nodeFolder) {
            File(
                CONNECTORS_FOLDER.getFile(), connectorPluginName
                .replace("%cloud_version%", CLOUD_VERSION)
                .replace("%build%", BUILD)
                .replace("%branch%", BRANCH)
            )
        }else {
            File(connectorFolder, connectorPluginName
                .replace("%cloud_version%", CLOUD_VERSION)
                .replace("%build%", BUILD)
                .replace("%branch%", BRANCH)
            )
        }
    }

    override fun getParsedConnectorURL(): URL {
        return URL(connectorDownloadUrl
            ?.replace("%cloud_version%", CLOUD_VERSION)
            ?.replace("%build%", BUILD)
            ?.replace("%branch%", BRANCH)
            ?: throw IllegalStateException("Connector download url is null!")
        )
    }

    override fun isUnknown(): Boolean = name.lowercase() == "unknown"

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

    override fun hashCode(): Int {
        var result = uniqueId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + versionHandlerName.hashCode()
        result = 31 * result + proxy.hashCode()
        result = 31 * result + defaultType.hashCode()
        result = 31 * result + connectorPluginName.hashCode()
        result = 31 * result + (connectorDownloadUrl?.hashCode() ?: 0)
        result = 31 * result + connectorFolder.hashCode()
        result = 31 * result + (libPattern?.hashCode() ?: 0)
        return result
    }

    fun copy(name: String): CloudServerVersionType {
        return CloudServerVersionType(
            uniqueId = uniqueId,
            name = name,
            versionHandlerName = versionHandlerName,
            proxy = proxy,
            defaultType = defaultType,
            connectorPluginName = connectorPluginName,
            connectorDownloadUrl = connectorDownloadUrl,
            connectorFolder = connectorFolder,
            libPattern = libPattern,
            jvmArguments = jvmArguments.toMutableList(),
            environmentVariables = environmentVariables.toMutableMap(),
            programParameters = programParameters.toMutableList(),
            defaultFiles = defaultFiles.toMutableMap(),
            fileEdits = fileEdits.toMutableMap()
        )
    }

}