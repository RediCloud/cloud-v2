package dev.redicloud.database.grid.map

import dev.redicloud.api.database.grid.map.ISyncedMutableMap
import dev.redicloud.database.DatabaseConnection

class SyncedMutableMap<K, V>(
    key: String,
    databaseConnection: DatabaseConnection
) : SyncedMap<K, V>(key, databaseConnection), ISyncedMutableMap<K, V> {

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