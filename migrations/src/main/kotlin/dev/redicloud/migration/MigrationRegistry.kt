package dev.redicloud.migration

import dev.redicloud.utils.version.CloudVersion

/**
 * Central registry for all known migrations.
 *
 * Migrations are registered explicitly at startup before the [MigrationRunner] executes.
 */
object MigrationRegistry {

    private val migrations = mutableListOf<Migration>()

    /** Register a migration. */
    fun register(migration: Migration) {
        migrations.add(migration)
    }

    /** Register multiple migrations at once. */
    fun registerAll(vararg migrations: Migration) {
        migrations.forEach { register(it) }
    }

    /**
     * Returns the migrations that need to run between [from] and [to], ordered correctly.
     *
     * - **Upgrade** (from < to): returns migrations where `version` is in `(from, to]`, ascending by version.
     * - **Downgrade** (from > to): returns migrations where `version` is in `(to, from]`, descending by version.
     * - **Same version**: returns an empty list.
     */
    fun getMigrationsBetween(from: CloudVersion, to: CloudVersion): List<Migration> {
        if (from == to) return emptyList()

        return if (from < to) {
            // Upgrade: run migrations for versions after 'from' up to and including 'to'
            migrations
                .filter { it.version > from && it.version <= to }
                .sortedBy { it.version }
        } else {
            // Downgrade: run migrations for versions after 'to' up to and including 'from'
            migrations
                .filter { it.version > to && it.version <= from }
                .sortedByDescending { it.version }
        }
    }

    /** Returns all registered migrations. */
    fun all(): List<Migration> = migrations.toList()

    /** Clears all registered migrations. Mainly useful for testing. */
    fun clear() {
        migrations.clear()
    }
}
