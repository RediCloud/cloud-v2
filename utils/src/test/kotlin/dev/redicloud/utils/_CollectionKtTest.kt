package dev.redicloud.utils

import kotlin.test.Test
import kotlin.test.assertTrue

class CollectionKtTest : UtilTest() {

    @Test
    fun takeFirstLastRandom1() = loopRandom {
            val list = listOf("entry1", "entry2", "entry3", "entry4", "entry5")
            val result = list.takeFirstLastRandom(3)
            assertTrue(
                result.size == 3
                        && result[0] == list.first()
                        && result[1] == list.last()
                        && list.contains(result[2])
                        && result[2] != list.first()
                        && result[2] != list.last(),
                "Unexpected result: $result"
            )
        }

    @Test
    fun takeFirstLastRandom2() = loopRandom {
        val list = listOf("entry1", "entry2", "entry3", "entry4", "entry5")
        val result = list.takeFirstLastRandom(5)
        assertTrue(
            result.size == 5
                && result[0] == list.first()
                && result[1] == list.last()
                && list.contains(result[2])
                && list.contains(result[3])
                && list.contains(result[4])
                && result[2] != list.first()
                && result[2] != list.last()
                && result[3] != list.first()
                && result[3] != list.last()
                && result[4] != list.first()
                && result[4] != list.last(),
            "Unexpected result: $result"
        )
    }

    @Test
    fun takeFirstLastRandom3() = loopRandom {
        val list = listOf("entry1")
        val result = list.takeFirstLastRandom(2)
        assertTrue (result.size == 1 && result[0] == list.first(), "Unexpected result: $result")
    }

}