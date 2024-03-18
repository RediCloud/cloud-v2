package dev.redicloud.repository.server.version

import dev.redicloud.api.version.ICloudServerVersion
import dev.redicloud.api.version.IServerVersion
import dev.redicloud.cache.IClusterCacheObject
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.version.serverversion.ServerVersion
import dev.redicloud.utils.ConfigurationFileEditor
import dev.redicloud.utils.gson.GsonInterface
import java.io.File
import java.util.UUID

class CloudServerVersion(
    override val uniqueId: UUID,
    override var typeId: UUID?,
    override var projectName: String,
    override var customDownloadUrl: String?,
    override var buildId: String?,
    @GsonInterface(ServerVersion::class)
    override var version: IServerVersion,
    override var javaVersionId: UUID?,
    override var libPattern: String? = null,
    override var patch: Boolean = false,
    override val online: Boolean = false,
    override var used: Boolean = false,
    override val jvmArguments: MutableList<String> = mutableListOf(),
    override val environmentVariables: MutableMap<String, String> = mutableMapOf(),
    override val programParameters: MutableList<String> = mutableListOf(),
    override val defaultFiles: MutableMap<String, String> = mutableMapOf(),
    override val fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
) : Comparable<CloudServerVersion>, IClusterCacheObject, ICloudServerVersion {

    companion object {
        private val logger = LogManager.Companion.logger(CloudServerVersion::class)
    }

    override val displayName: String
        get() {
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

    fun copy(name: String): CloudServerVersion {
        return CloudServerVersion(
            UUID.randomUUID(),
            typeId,
            name,
            customDownloadUrl,
            buildId,
            version,
            javaVersionId,
            libPattern,
            patch,
            false,
            used,
            jvmArguments.toMutableList(),
            environmentVariables.toMutableMap(),
            programParameters.toMutableList(),
            defaultFiles.toMutableMap(),
            fileEdits.toMutableMap()
        )
    }

}