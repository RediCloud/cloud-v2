package dev.redicloud.database.grid.map

import dev.redicloud.api.database.grid.map.ISyncedMap
import dev.redicloud.database.DatabaseConnection
import org.redisson.api.RMap

/**
 * Read-only map backed by a Redisson [RMap], synchronized with Redis.
 *
 * All read operations delegate directly to the distributed map identified by [key].
 *
 * @param K the key type
 * @param V the value type
 * @param key the Redis key identifying this map
 * @param databaseConnection the database connection providing the Redisson client
 */
open class SyncedMap<K, V>(
    final override val key: String,
    databaseConnection: DatabaseConnection
) : ISyncedMap<K, V> {

    /** The underlying Redisson map handle. */
    protected val handle: RMap<K, V> = databaseConnection.client.getMap(key)

    override val size: Int
        get() {
            return handle.size
        }

    override val entries: Set<Map.Entry<K, V>>
        get() {
            return handle.entries
        }

    override val keys: Set<K>
        get() {
            return handle.keys
        }

    override val values: Collection<V>
        get() {
            return handle.values
        }

    override fun isEmpty(): Boolean {
        return handle.isEmpty()
    }

    override fun get(key: K): V? {
        return handle[key]
    }

    override fun containsValue(value: V): Boolean {
        return handle.containsValue(value)
    }

    override fun containsKey(key: K): Boolean {
        return handle.containsKey(key)
    }
}
