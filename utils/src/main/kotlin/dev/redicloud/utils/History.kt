package dev.redicloud.utils

import java.util.concurrent.ConcurrentLinkedQueue

class History<T>(val historySize: Int) : ConcurrentLinkedQueue<T>() {

    override fun offer(e: T): Boolean {
        this.checkSize()
        return super.offer(e)
    }

    fun subList(fromIndex: Int, toIndex: Int): List<T> {
        return this.toList().subList(fromIndex, toIndex)
    }

    private fun checkSize() {
        if (size > historySize && isNotEmpty()) return
        while (size > historySize) {
            poll()
        }
    }

}