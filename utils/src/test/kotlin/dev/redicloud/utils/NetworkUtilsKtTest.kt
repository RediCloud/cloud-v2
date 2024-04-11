package dev.redicloud.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkUtilsKtTest : UtilTest() {

    @Test
    fun getAllAddressesTest() {
        val addresses = getAllAddresses()
        assert(addresses.isNotEmpty())
        if (addresses.any { isIpv6(it) }) {
            assertTrue(addresses.contains("0:0:0:0:0:0:0:1"), "Localhost ipv6 not found in addresses")
        }
        assertTrue(addresses.contains("127.0.0.1"), "Localhost ipv4 not found in addresses")
    }

    @Test
    fun getAllIpV4Test() {
        val ipV4 = getAllIpV4()
        assert(ipV4.isNotEmpty())
        assertFalse(ipV4.any { isIpv6(it) }, "Found ipv6 in ipV4")
        assertTrue(ipV4.contains("127.0.0.1"), "Localhost ipv4 not found in ipV4")
    }

    @Test
    fun isIpv4Test() {
        val ipv4 = generateRandomIPv4()
        val random = randomString(20)
        val ipv6 = generateRandomIPv6()
        assertTrue(isIpv4(ipv4), "Failed to validate ipv4")
        assertFalse(isIpv4(ipv6), "Failed to invalidate ipv6")
        assertFalse(isIpv4(random), "Failed to invalidate random string")
    }

    @Test
    fun isIpv6Test() {
        val ipv6 = generateRandomIPv6()
        val random = randomString(20)
        val ipv4 = generateRandomIPv4()
        assertTrue(isIpv6(ipv6), "Failed to validate ipv6")
        assertFalse(isIpv6(ipv4), "Failed to invalidate ipv4")
        assertFalse(isIpv6(random), "Failed to invalidate random string")
    }

    private fun generateRandomIPv6(): String {
        val hexChars = "0123456789abcdef"
        val segments = List(8) { _ ->
            (1..4).map { hexChars.random() }.joinToString("")
        }
        return segments.joinToString(":")
    }

    private fun generateRandomIPv4(): String {
        val segments = List(4) { _ ->
            (0..255).random()
        }
        return segments.joinToString(".")
    }
}