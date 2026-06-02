package dev.redicloud.database.grid.map.cache

import dev.redicloud.api.database.grid.map.cache.ISyncedCacheMap
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.grid.map.SyncedMap

open class SyncedCacheMap<K, V>(
    key: String,
    databaseConnection: DatabaseConnection
) : SyncedMap<K, V>(key, databaseConnection), ISyncedCacheMap<K, V>
