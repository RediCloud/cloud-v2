package dev.redicloud.api.version

import dev.redicloud.api.utils.ProcessConfiguration
import java.io.File
import java.net.URL
import java.util.*

/**
 * Defines a server version type (e.g. Paper, Velocity, Waterfall).
 * A type groups multiple [ICloudServerVersion]s and provides shared configuration
 * such as connector plugin details and process-level settings inherited from [ProcessConfiguration].
 */
interface ICloudServerVersionType : ProcessConfiguration {

    /** Unique identifier of this server version type. */
    val uniqueId: UUID

    /** Human-readable name of this type (e.g. `paper`, `velocity`). */
    var name: String

    /** Name of the [IServerVersionHandler] responsible for downloading and managing versions of this type. */
    var versionHandlerName: String

    /** Whether this type represents a proxy server (e.g. Velocity, BungeeCord). */
    var proxy: Boolean

    /** Whether this is a built-in default type that cannot be deleted. */
    val defaultType: Boolean

    /** File name of the connector plugin jar (supports `%cloud_version%`, `%build%`, `%branch%` placeholders). */
    var connectorPluginName: String

    /** Download URL for the connector plugin (supports `%cloud_version%`, `%build%`, `%branch%` placeholders). */
    var connectorDownloadUrl: String?

    /** Target folder inside the server directory where the connector plugin is placed (e.g. `plugins`). */
    var connectorFolder: String

    /** Regex pattern matching library files that should be preserved during server preparation. */
    var libPattern: String?

    /** Optional SHA-256 hash for connector integrity verification. */
    var connectorSha256: String?

    /**
     * Returns the resolved [File] path to the connector plugin jar.
     *
     * @param nodeFolder `true` to resolve relative to the node's connectors folder,
     *                   `false` to resolve relative to the server's [connectorFolder]
     * @return the connector plugin file
     */
    fun getParsedConnectorFile(nodeFolder: Boolean): File

    /**
     * Returns the resolved download [URL] for the connector plugin with all placeholders replaced.
     *
     * @return the connector download URL
     * @throws IllegalStateException if [connectorDownloadUrl] is `null`
     */
    fun getParsedConnectorURL(): URL

    /**
     * Whether this type is the special "unknown" placeholder type.
     *
     * @return `true` if the [name] equals "unknown" (case-insensitive)
     */
    fun isUnknown(): Boolean
}
