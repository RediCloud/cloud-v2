package dev.redicloud.repository.server.version

import dev.redicloud.cache.IClusterCacheObject
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.ConfigurationFileEditor
import dev.redicloud.utils.ProcessConfiguration
import java.io.File
import java.util.UUID

class CloudServerVersion(
    val uniqueId: UUID,
    var typeId: UUID?,
    var projectName: String,
    var customDownloadUrl: String?,
    var buildId: String?,
    var version: ServerVersion,
    var javaVersionId: UUID?,
    var libPattern: String? = null,
    var patch: Boolean = false,
    val online: Boolean = false,
    var used: Boolean = false,
    jvmArguments: MutableList<String> = mutableListOf(),
    environmentVariables: MutableMap<String, String> = mutableMapOf(),
    programmParameters: MutableList<String> = mutableListOf(),
    defaultFiles: MutableMap<String, String> = mutableMapOf(),
    fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
): ProcessConfiguration(
    jvmArguments,
    environmentVariables,
    programmParameters,
    defaultFiles,
    fileEdits
), Comparable<CloudServerVersion>, IClusterCacheObject {

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

    fun isSimilar(other: CloudServerVersion): Boolean {
        return typeId == other.typeId &&
                projectName == other.projectName &&
                customDownloadUrl == other.customDownloadUrl &&
                version.name == other.version.name &&
                libPattern == other.libPattern &&
                patch == other.patch &&
                online == other.online &&
                defaultFiles == other.defaultFiles &&
                fileEdits == other.fileEdits
    }

    override fun compareTo(other: CloudServerVersion): Int {
        return version.compareTo(other.version)
    }

}