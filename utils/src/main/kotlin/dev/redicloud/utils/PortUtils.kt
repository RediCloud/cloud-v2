package dev.redicloud.utils

import java.net.ServerSocket

private val blocked = mutableListOf<Int>()

fun findFreePort(startPort: Int, endPort: Int, random: Boolean = true): Int {
    return if (random) {
        (startPort..endPort).toMutableList().also { it.shuffle() }
            .firstOrNull { isPortFree(it) } ?: -1
    } else {
        (startPort..endPort).toMutableList().firstOrNull { isPortFree(it) } ?: -1
    }
}

fun blockPort(port: Int) {
    blocked.add(port)
}

fun freePort(port: Int) {
    blocked.remove(port)
}

fun findFreePort(startPort: Int, random: Boolean = true): Int {
    return findFreePort(startPort, startPort + 1000, random)
}

fun findFreePort(range: IntRange): Int {
    return range.toMutableList().also { it.shuffle() }.firstOrNull { isPortFree(it) } ?: -1
}

fun isPortFree(port: Int): Boolean {
    if (blocked.contains(port)) return false
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