package dev.redicloud.api.database.grid.map

interface ISyncedMap<K, V> : Map<K, V> {
    val key: String
}