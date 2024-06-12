package dev.redicloud.utils

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock

class SimpleLock : Lock {

    private val state = AtomicBoolean(false)
    val isLocked: Boolean
        get() = state.get()

    override fun lock() {
        while (!state.compareAndSet(false, true)) {
            Thread.yield()
        }
    }

    override fun lockInterruptibly() {
        while (!state.compareAndSet(false, true)) {
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            Thread.yield()
        }
    }

    override fun tryLock(): Boolean {
        return state.compareAndSet(false, true)
    }

    override fun tryLock(time: Long, unit: TimeUnit): Boolean {
        val end = System.currentTimeMillis() + unit.toMillis(time)
        while (!state.compareAndSet(false, true)) {
            if (System.currentTimeMillis() >= end) {
                return false
            }
            Thread.yield()
        }
        return true
    }

    override fun unlock() {
        state.set(false)
    }

    override fun newCondition(): Condition {
        throw UnsupportedOperationException()
    }
}