package dev.redicloud.utils

import kotlin.test.Test
import kotlin.test.assertEquals


class HistoryTest : UtilTest() {

    @Test
    fun autoRemoveTest() {
        val history = History<String>(5)
        for (i in 0..10) {
            history.add(i.toString())
        }
        assertEquals(5, history.size, "History size should be 5")
        for (i in 6..10) {
            assertEquals(i.toString(), history.subList(0, 5)[i - 6], "History should contain $i")
        }
    }

}