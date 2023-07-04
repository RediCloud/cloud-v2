package dev.redicloud.utils

import java.util.concurrent.ConcurrentHashMap

class History<T : Any>(val historySize: Int) {

    private val map = ConcurrentHashMap<Long, T>()

    val size: Int
        get() = map.size

    fun add(value: T) {
        map[System.nanoTime()] = value
        checkSize()
    }

    fun remove(key: Long, value: T) {
        map.remove(key, value)
    }

    fun subList(fromIndex: Int, toIndex: Int): List<T> {
        val f = if (fromIndex < 0) 0 else fromIndex
        val t = if (toIndex > size) size else toIndex
        return map.values.toList().subList(f, t)
    }

    private fun checkSize() {
        if (size > historySize && map.isNotEmpty()) return
        while (size > historySize) {
            map.minBy { it.key }.let {
                map.remove(it.key)
            }
        }
    }

    fun forEach(action: (T) -> Unit) {
        map.toSortedMap { o1, o2 -> o1.compareTo(o2) }.values.toList().forEach(action)
    }

}