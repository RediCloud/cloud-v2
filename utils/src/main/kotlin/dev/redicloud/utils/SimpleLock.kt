package dev.redicloud.utils

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock

class SimpleLock : Lock {

    private val state = AtomicBoolean(false)
    private val lockThreadId = ThreadLocal<Long>()
    val isLocked: Boolean
        get() = state.get()

    override fun lock() {
        if (lockThreadId.get() == Thread.currentThread().id) {
            return
        }
        while (!state.compareAndSet(false, true)) {
            try {
                Thread.sleep(2)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        lockThreadId.set(Thread.currentThread().id)
    }

    override fun lockInterruptibly() {
        if (lockThreadId.get() == Thread.currentThread().id) {
            return
        }
        while (!state.compareAndSet(false, true)) {
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            try {
                Thread.sleep(2)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        lockThreadId.set(Thread.currentThread().id)
    }

    override fun tryLock(): Boolean {
        val result = state.compareAndSet(false, true)
        if (result) {
            lockThreadId.set(Thread.currentThread().id)
        }
        return result
    }

    override fun tryLock(time: Long, unit: TimeUnit): Boolean {
        if (lockThreadId.get() == Thread.currentThread().id) {
            return true
        }
        val end = System.currentTimeMillis() + unit.toMillis(time)
        while (!state.compareAndSet(false, true)) {
            if (System.currentTimeMillis() >= end) {
                return false
            }
            try {
                Thread.sleep(2)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        lockThreadId.set(Thread.currentThread().id)
        return true
    }

    override fun unlock() {
        state.set(false)
        lockThreadId.set(null)
    }

    override fun newCondition(): Condition {
        throw UnsupportedOperationException()
    }
}