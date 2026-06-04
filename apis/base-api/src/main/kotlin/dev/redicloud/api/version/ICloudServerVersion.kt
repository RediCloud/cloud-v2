package dev.redicloud.api.version

import dev.redicloud.api.utils.ProcessConfiguration
import java.util.*

/**
 * Represents a specific server version (e.g. Paper 1.20.4 build #123).
 * Inherits process configuration (JVM args, env vars, etc.) from [ProcessConfiguration].
 */
interface ICloudServerVersion : ProcessConfiguration {

    /** Unique identifier of this server version. */
    val uniqueId: UUID

    /** ID of the [ICloudServerVersionType] this version belongs to, or `null` if unset. */
    var typeId: UUID?

    /** Project name used by the version handler to resolve downloads (e.g. `paper`). */
    var projectName: String

    /** Optional custom download URL overriding the version handler's default resolution. */
    var customDownloadUrl: String?

    /** Build identifier within the project (e.g. `123`), or `null` if not applicable. */
    var buildId: String?

    /** The Minecraft [IServerVersion] (e.g. `1.20.4`) associated with this server version. */
    var version: IServerVersion

    /** ID of the Java version required to run this server version, or `null` for auto-detection. */
    var javaVersionId: UUID?

    /** Regex pattern matching library files that should be preserved during server preparation. */
    var libPattern: String?

    /** Whether this version is a patch that is applied on top of an existing installation. */
    var patch: Boolean

    /** Whether this version's jar file is currently available on disk. */
    val online: Boolean

    /** Whether this version is currently in use by at least one configuration template. */
    val used: Boolean

    /** Human-readable display name combining [projectName] and [version] (e.g. `paper_1.20.4`). */
    val displayName: String
}
