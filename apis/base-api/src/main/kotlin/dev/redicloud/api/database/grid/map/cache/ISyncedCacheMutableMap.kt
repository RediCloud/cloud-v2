package dev.redicloud.api.database.grid.map.cache

import dev.redicloud.api.database.grid.map.ISyncedMutableMap

interface ISyncedCacheMutableMap<K, V> : ISyncedCacheMap<K, V>, ISyncedMutableMap<K, V>