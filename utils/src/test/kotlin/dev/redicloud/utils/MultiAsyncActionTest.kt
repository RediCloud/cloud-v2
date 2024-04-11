package dev.redicloud.utils

import kotlinx.coroutines.runBlocking
import kotlin.test.Test


class MultiAsyncActionTest : UtilTest() {

    @Test
    fun joinAllTest() {
        val taskStates = mutableMapOf<String, Boolean>()
        val multiAsyncAction = MultiAsyncAction()
        for (i in 0..10) {
            multiAsyncAction.add {
                taskStates[i.toString()] = false
                Thread.sleep(randomIntInRange(100, 1000).toLong())
            }
        }
        runBlocking { multiAsyncAction.joinAll() }
        for (i in 0..10) {
            assert(taskStates[i.toString()] == false)
        }
    }

}