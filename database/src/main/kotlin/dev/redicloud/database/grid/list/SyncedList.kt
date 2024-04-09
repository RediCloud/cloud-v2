package dev.redicloud.database.grid.list

import dev.redicloud.api.database.grid.list.ISyncedList
import dev.redicloud.database.DatabaseConnection
import org.redisson.api.RList

open class SyncedList<E>(
    final override val key: String,
    databaseConnection: DatabaseConnection
) : ISyncedList<E> {

    protected val handle: RList<E> = databaseConnection.client.getList(key)

    override val size: Int
        get() {
            return handle.size
        }

    override fun get(index: Int): E {
        return handle[index]
    }

    override fun isEmpty(): Boolean {
        return handle.isEmpty()
    }

    override fun iterator(): Iterator<E> {
        return handle.iterator()
    }

    override fun listIterator(): ListIterator<E> {
        return handle.listIterator()
    }

    override fun listIterator(index: Int): ListIterator<E> {
        return handle.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> {
        return handle.subList(fromIndex, toIndex)
    }

    override fun lastIndexOf(element: E): Int {
        return handle.lastIndexOf(element)
    }

    override fun indexOf(element: E): Int {
        return handle.indexOf(element)
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return handle.containsAll(elements)
    }

    override fun contains(element: E): Boolean {
        return handle.contains(element)
    }

}