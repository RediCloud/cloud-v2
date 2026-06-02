package dev.redicloud.migration

/**
 * Defines where a migration operates.
 */
enum class MigrationType {

    /**
     * Modifies shared Redis data (keys, values, structures).
     * Executed **once** across the entire cluster, protected by a distributed lock.
     */
    DATABASE,

    /**
     * Modifies local file/folder structure on each node.
     * Executed **on every node** independently.
     */
    LOCAL
}
