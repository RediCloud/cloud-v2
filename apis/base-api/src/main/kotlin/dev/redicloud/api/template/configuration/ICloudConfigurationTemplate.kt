package dev.redicloud.api.template.configuration

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.utils.ProcessConfiguration
import java.util.UUID

/**
 * Describes a configuration template that defines how cloud servers are created and managed.
 * Each template specifies resource limits, scaling rules, and process configuration
 * (JVM arguments, environment variables, etc.) inherited from [ProcessConfiguration].
 */
interface ICloudConfigurationTemplate : Comparable<ICloudConfigurationTemplate>, ProcessConfiguration {

    /** Unique identifier of this configuration template. */
    val uniqueId: UUID

    /** Human-readable name of this configuration template. */
    val name: String

    /** Maximum heap memory in megabytes allocated to servers created from this template. */
    val maxMemory: Long

    /** IDs of file templates whose contents are copied into the server directory before startup. */
    val fileTemplateIds: MutableList<UUID>

    /** IDs of nodes that are allowed to host servers of this template. Empty means all nodes. */
    val nodeIds: MutableList<ServiceId>

    /** Minimum number of servers that must be running across the entire cluster. */
    var minStartedServices: Int

    /** Maximum number of servers allowed across the cluster. `-1` means unlimited. */
    var maxStartedServices: Int

    /** Minimum number of servers that must be running on each individual node. */
    var minStartedServicesPerNode: Int

    /** Maximum number of servers allowed per node. `-1` means unlimited. */
    var maxStartedServicesPerNode: Int

    /** Player-fill percentage threshold at which a new server is started automatically. */
    var percentToStartNewService: Double

    /** Delimiter used between the template name and the server index (e.g. `Lobby-1`). */
    var serverSplitter: String

    /** Whether servers of this template act as fallback/lobby servers. */
    var fallbackServer: Boolean

    /** Start priority; lower values are started first. Fallback servers default to `0`. */
    var startPriority: Int

    /** ID of the [ICloudServerVersion] used by servers of this template, or `null` if unset. */
    var serverVersionId: UUID?

    /** Whether servers of this template are static (their working directory is preserved across restarts). */
    var static: Boolean

    /** Fixed port for this template's servers. `-1` means the port is assigned automatically. */
    var startPort: Int

    /** Permission required to join servers of this template, or `null` if unrestricted. */
    var joinPermission: String?

    /** Maximum number of players allowed on a single server. */
    var maxPlayers: Int

    /** Duration in milliseconds after which an empty (useless) server is stopped automatically. */
    var timeAfterStopUselessServer: Long
}
