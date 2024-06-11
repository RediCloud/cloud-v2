package dev.redicloud.utils

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringExtensionsTest : UtilTest() {

    @Test
    fun isUUID1() {
        assertFalse(randomString(5).isUUID(), "Expected 'test' to not be a UUID")
    }

    @Test
    fun isUUID2() {
        assertTrue(UUID.randomUUID().toString().isUUID(), "Expected 'UUID.randomUUID()' to be a UUID")
    }

    @Test
    fun toUUID() {
        val uniqueIdString = randomString(5).toUUID().toString()
        runCatching { UUID.fromString(uniqueIdString) }.onFailure {
            throw AssertionError("Expected '$uniqueIdString' to be a valid UUID")
        }
    }

    @Test
    fun toBase64() {
        val base64 = randomString(20).toBase64()
        assertTrue(base64.isNotEmpty(), "Expected base64 to not be empty")
        assertTrue(base64.endsWith("="), "Expected base64 to end with '=' but was '$base64'")
    }

    @Test
    fun fromBase64() {
        val initValue = randomString(20)
        val base64 = initValue.toBase64()
        val original = base64.fromBase64()
        assertTrue(original.isNotEmpty(), "Expected original to not be empty")
        assertEquals(original, initValue, "Expected original to be '$initValue' but was '$original'")
    }
}