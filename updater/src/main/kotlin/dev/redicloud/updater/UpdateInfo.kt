package dev.redicloud.updater

/**
 * Persisted to `.update-info` when a version switch is pending.
 *
 * On next startup the [Updater] reads this file, cleans up old JARs,
 * and deletes the file.
 */
data class UpdateInfo(
    val newVersion: String,
    val oldVersion: String
)
