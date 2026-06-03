package dev.redicloud.migration

import dev.redicloud.utils.version.CloudVersion

/**
 * Result of [MigrationRunner.run], indicating what action the caller should take.
 */
sealed class MigrationResult {

    /** Migrations completed successfully (or none were needed). Startup can continue. */
    data object Success : MigrationResult()

    /**
     * The cluster schema version is ahead of this node's JAR version.
     * The caller must upgrade the node to [targetVersion] and restart.
     */
    data class UpgradeRequired(val targetVersion: CloudVersion) : MigrationResult()
}
