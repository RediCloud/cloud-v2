package dev.redicloud.utils

import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertTrue

class ConcurrentBatchTest : UtilTest() {

    @Test
    fun joinAllTest() {
        val taskStates = ConcurrentHashMap<String, Boolean>()
        val batch = ConcurrentBatch()
        for (i in 0..10) {
            batch.add {
                taskStates[i.toString()] = true
                Thread.sleep(randomIntInRange(100, 1000).toLong())
            }
        }
        runBlocking { batch.joinAll() }
        for (i in 0..10) {
            assertTrue(taskStates[i.toString()] == true, "Task $i did not execute")
        }
    }
}
