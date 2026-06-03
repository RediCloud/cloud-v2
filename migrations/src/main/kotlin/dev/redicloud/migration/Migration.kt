package dev.redicloud.migration

import dev.redicloud.utils.version.CloudVersion

/**
 * A single migration step that upgrades or downgrades data/files between cloud versions.
 *
 * Implementations must be registered in [MigrationRegistry].
 */
interface Migration {

    /** The version this migration upgrades **to**. */
    val version: CloudVersion

    /** Whether this migration operates on shared Redis data or local files. */
    val type: MigrationType

    /** Human-readable description of what this migration does. */
    val description: String

    /** Run the upgrade migration. */
    suspend fun up(context: MigrationContext)

    /** Run the downgrade migration (rollback). */
    suspend fun down(context: MigrationContext)
}
