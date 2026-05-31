package dev.redicloud.utils

import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ConcurrentBatchTest : UtilTest() {

    @Test
    fun joinAllTest() {
        val taskStates = mutableMapOf<String, Boolean>()
        val batch = ConcurrentBatch()
        for (i in 0..10) {
            batch.add {
                taskStates[i.toString()] = false
                Thread.sleep(randomIntInRange(100, 1000).toLong())
            }
        }
        runBlocking { batch.joinAll() }
        for (i in 0..10) {
            assert(taskStates[i.toString()] == false)
        }
    }
}
