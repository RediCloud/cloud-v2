package dev.redicloud.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PortUtilsTest : UtilTest() {

    @Test
    fun blockPortTest() {
        val port = randomIntInRange(55535, 65535)
        assert(port in 55535..65535)
        blockPort(port)
        assertFalse(isPortFree(port))
    }

    @Test
    fun freePortTest() {
        val port = randomIntInRange(55535, 65535)
        assert(port in 55535..65535)
        blockPort(port)
        freePort(port)
        assert(isPortFree(port))
    }

    @Test
    fun findFreePortTest() {
        val startPort = randomIntInRange(55535, 65535)
        assert(startPort in 55535..65535)
        blockPort(startPort)
        val port = findFreePort(startPort, random = false)
        assert(port != -1)
        assert(isPortFree(port))
        assertEquals(startPort + 1, port)
    }

}