package dev.redicloud.repository.server.version

import dev.redicloud.api.cache.IClusterCacheObject
import dev.redicloud.api.version.ICloudServerVersion
import dev.redicloud.api.version.IServerVersion
import dev.redicloud.repository.server.version.serverversion.ServerVersion
import dev.redicloud.utils.gson.GsonInterface
import java.util.UUID

/**
 * Default implementation of [ICloudServerVersion].
 * Represents a downloadable server version (e.g. Paper 1.20.4 build #123)
 * with process configuration inherited from [ProcessConfiguration][dev.redicloud.api.utils.ProcessConfiguration].
 */
@Suppress("LongParameterList")
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

    override val displayName: String
        get() {
            return "${projectName}_${version.name}"
        }

    /**
     * Checks whether this version is structurally similar to [other],
     * comparing type, project, download URL, version name, lib pattern,
     * patch flag, online state, default files, and file edits.
     */
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

    /**
     * Creates a deep copy of this version with a new [projectName][name] and a fresh [uniqueId].
     * Mutable collections are copied so the new instance is independent.
     */
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
