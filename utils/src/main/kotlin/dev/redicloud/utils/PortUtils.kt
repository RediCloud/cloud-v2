package dev.redicloud.utils

import java.net.ServerSocket

fun findFreePort(startPort: Int, endPort: Int): Int {
    return (startPort..endPort).toMutableList().also { it.shuffle() }.firstOrNull { isPortFree(it) } ?: -1
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