package dev.redicloud.api.database.grid.map

interface ISyncedMutableMap<K, V> : ISyncedMap<K, V>, MutableMap<K, V>