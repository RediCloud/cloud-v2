package dev.redicloud.api.files.creator

import dev.redicloud.utils.toUUID

fun main() {
    while (true) {
        println("Format: <type>_<version> (paper_1.20.4)")
        print("> ")
        val input = readlnOrNull() ?: break
        if (input == "exit") break
        val id = input.toUUID()
        println(id)
    }
}

