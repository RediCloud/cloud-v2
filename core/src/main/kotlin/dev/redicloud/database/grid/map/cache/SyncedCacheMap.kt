package dev.redicloud.database.grid.map.cache

import dev.redicloud.api.database.grid.map.cache.ISyncedCacheMap
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.grid.map.SyncedMap

/**
 * Read-only cached map backed by a Redisson map, synchronized with Redis.
 *
 * Combines [SyncedMap] read semantics with the [ISyncedCacheMap] cache contract.
 *
 * @param K the key type
 * @param V the value type
 * @param key the Redis key identifying this map
 * @param databaseConnection the database connection providing the Redisson client
 */
open class SyncedCacheMap<K, V>(
    key: String,
    databaseConnection: DatabaseConnection
) : SyncedMap<K, V>(key, databaseConnection), ISyncedCacheMap<K, V>
