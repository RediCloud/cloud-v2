package dev.redicloud.api.database.grid.list

interface ISyncedList<E> : List<E> {
    val key: String
}