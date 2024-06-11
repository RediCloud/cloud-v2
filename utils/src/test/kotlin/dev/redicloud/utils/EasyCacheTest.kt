package dev.redicloud.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds


class EasyCacheTest : UtilTest() {

    @Test
    fun testGet() {
        val cache = SingleCache(10.seconds) { "test" }
        assertEquals("test", cache.get(), "Cache should return the value")
    }

    @Test
    fun testTimeout() {
        var getCount = 0
        val cache = SingleCache(1.seconds) {
            getCount++
            "test"
        }
        assertEquals("test", cache.get(), "Cache should return the value")
        Thread.sleep(2000)
        assertEquals("test", cache.get(), "Cache should return the value")
        assertEquals(2, getCount, "Cache should have been called twice")
    }

    @Test
    fun testCache() {
        var getCount = 0
        val cache = SingleCache(5.seconds) {
            getCount++
            "test"
        }
        assertEquals("test", cache.get(), "Cache should return the value")
        Thread.sleep(1000)
        assertEquals("test", cache.get(), "Cache should return the value")
        assertEquals(1, getCount, "Cache should have been called once")
    }

}