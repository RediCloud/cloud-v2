package dev.redicloud.utils

import java.net.ServerSocket

fun getFreePort(startPort: Int, endPort: Int): Int {
    var port = -1

    for (currentPort in startPort..endPort) {
        if (isPortFree(currentPort)) {
            port = currentPort
            break
        }
    }

    return port
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