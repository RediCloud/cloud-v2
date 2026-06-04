package dev.redicloud.database.grid.map.cache

import dev.redicloud.api.database.grid.map.cache.ISyncedCacheMutableMap
import dev.redicloud.database.DatabaseConnection

/**
 * Mutable cached map backed by a Redisson map, synchronized with Redis.
 *
 * Extends [SyncedCacheMap] with write operations that propagate to the
 * distributed Redis store.
 *
 * @param K the key type
 * @param V the value type
 * @param key the Redis key identifying this map
 * @param databaseConnection the database connection providing the Redisson client
 */
class SyncedCacheMutableMap<K, V>(
    key: String,
    databaseConnection: DatabaseConnection
) : SyncedCacheMap<K, V>(key, databaseConnection), ISyncedCacheMutableMap<K, V> {

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            return handle.entries
        }

    override val keys: MutableSet<K>
        get() {
            return handle.keys
        }

    override val values: MutableCollection<V>
        get() {
            return handle.values
        }

    override fun clear() {
        handle.clear()
    }

    override fun remove(key: K): V? {
        return handle.remove(key)
    }

    override fun putAll(from: Map<out K, V>) {
        handle.putAll(from)
    }

    override fun put(key: K, value: V): V? {
        return handle.put(key, value)
    }
}
