package dev.redicloud.api.utils

import java.io.InputStream

interface ProcessHandler {

    val inputStream: InputStream
    val errorStream: InputStream
    val logged: List<String>

    suspend fun onExit(): Int

    fun onExit(block: (Int) -> Unit)

    fun onLine(block: (String) -> Unit)

}