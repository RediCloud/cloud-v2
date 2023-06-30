package dev.redicloud.utils

import java.net.ServerSocket

fun findFreePort(startPort: Int, endPort: Int, random: Boolean = true): Int {
    return if (random) {
        (startPort..endPort).toMutableList().also { it.shuffle() }.firstOrNull { isPortFree(it) } ?: -1
    }else {
        (startPort..endPort).toMutableList().firstOrNull { isPortFree(it) } ?: -1
    }
}

fun findFreePort(startPort: Int, random: Boolean = true): Int {
    return findFreePort(startPort, startPort + 1000, random)
}

fun findFreePort(range: IntRange): Int {
    return range.toMutableList().also { it.shuffle() }.firstOrNull { isPortFree(it) } ?: -1
}

fun isPortFree(port: Int): Boolean {
    var serverSocket: ServerSocket? = null

    return try {
        serverSocket = ServerSocket(port)
        true
    } catch (_: Exception) {
        false
    } finally {
        serverSocket?.close()
    }
}