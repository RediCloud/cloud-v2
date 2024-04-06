package dev.redicloud.database.grid.list

import dev.redicloud.api.database.grid.list.ISyncedMutableList
import dev.redicloud.database.DatabaseConnection

class SyncedMutableList<E>(
    key: String,
    databaseConnection: DatabaseConnection
) : SyncedList<E>(key, databaseConnection), ISyncedMutableList<E> {

    override fun add(element: E): Boolean {
        return handle.add(element)
    }

    override fun add(index: Int, element: E) {
        handle.add(index, element)
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        return handle.addAll(index, elements)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        return handle.addAll(elements)
    }

    override fun clear() {
        handle.clear()
    }

    override fun iterator(): MutableIterator<E> {
        return handle.iterator()
    }

    override fun listIterator(): MutableListIterator<E> {
        return handle.listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        return handle.listIterator(index)
    }

    override fun removeAt(index: Int): E {
        return handle.removeAt(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        return handle.subList(fromIndex, toIndex)
    }

    override fun set(index: Int, element: E): E {
        return handle.set(index, element)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return handle.retainAll(elements)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return handle.removeAll(elements)
    }

    override fun remove(element: E): Boolean {
        return handle.remove(element)
    }

}