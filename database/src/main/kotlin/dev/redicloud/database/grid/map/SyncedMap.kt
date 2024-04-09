package dev.redicloud.database.grid.map

import dev.redicloud.api.database.grid.map.ISyncedMap
import dev.redicloud.database.DatabaseConnection
import org.redisson.api.RMap

open class SyncedMap<K, V>(
    final override val key: String,
    databaseConnection: DatabaseConnection
) : ISyncedMap<K, V> {

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