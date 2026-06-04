package dev.redicloud.repository.server.version

import dev.redicloud.api.cache.IClusterCacheObject
import dev.redicloud.api.utils.CONNECTORS_FOLDER
import dev.redicloud.api.version.ICloudServerVersionType
import dev.redicloud.utils.*
import java.io.File
import java.net.URL
import java.util.*

/**
 * Default implementation of [ICloudServerVersionType].
 * Represents a server platform type (e.g. Paper, Velocity) and provides connector
 * plugin details along with process configuration inherited from
 * [ProcessConfiguration][dev.redicloud.api.utils.ProcessConfiguration].
 */
@Suppress("LongParameterList")
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
    override var connectorSha256: String? = null,
    override val jvmArguments: MutableList<String> = mutableListOf(),
    override val environmentVariables: MutableMap<String, String> = mutableMapOf(),
    override val programParameters: MutableList<String> = mutableListOf(),
    override val defaultFiles: MutableMap<String, String> = mutableMapOf(),
    override val fileEdits: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
) : IClusterCacheObject, ICloudServerVersionType {

    override fun getParsedConnectorFile(nodeFolder: Boolean): File {
        return if (nodeFolder) {
            File(
                CONNECTORS_FOLDER.getFile(),
                connectorPluginName
                    .replace("%cloud_version%", CLOUD_VERSION)
                    .replace("%build%", BUILD)
                    .replace("%branch%", BRANCH)
            )
        } else {
            File(
                connectorFolder,
                connectorPluginName
                    .replace("%cloud_version%", CLOUD_VERSION)
                    .replace("%build%", BUILD)
                    .replace("%branch%", BRANCH)
            )
        }
    }

    override fun getParsedConnectorURL(): URL {
        return URL(
            connectorDownloadUrl
                ?.replace("%cloud_version%", CLOUD_VERSION)
                ?.replace("%build%", BUILD)
                ?.replace("%branch%", BRANCH)
                ?: error("Connector download url is null!")
        )
    }

    override fun isUnknown(): Boolean = name.lowercase() == "unknown"

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
        if (libPattern != other.libPattern) return false
        return connectorSha256 == other.connectorSha256
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
        result = 31 * result + (connectorSha256?.hashCode() ?: 0)
        result = 31 * result + jvmArguments.hashCode()
        result = 31 * result + environmentVariables.hashCode()
        result = 31 * result + programParameters.hashCode()
        result = 31 * result + defaultFiles.hashCode()
        result = 31 * result + fileEdits.hashCode()
        return result
    }

    /**
     * Creates a deep copy of this type with a new [name] and a fresh [uniqueId].
     * Mutable collections are copied so the new instance is independent.
     * The [defaultType] flag is always set to `false` on copies.
     */
    fun copy(name: String): CloudServerVersionType {
        return CloudServerVersionType(
            uniqueId = UUID.randomUUID(),
            name = name,
            versionHandlerName = versionHandlerName,
            proxy = proxy,
            defaultType = false,
            connectorPluginName = connectorPluginName,
            connectorDownloadUrl = connectorDownloadUrl,
            connectorFolder = connectorFolder,
            libPattern = libPattern,
            connectorSha256 = connectorSha256,
            jvmArguments = jvmArguments.toMutableList(),
            environmentVariables = environmentVariables.toMutableMap(),
            programParameters = programParameters.toMutableList(),
            defaultFiles = defaultFiles.toMutableMap(),
            fileEdits = fileEdits.toMutableMap()
        )
    }
}
