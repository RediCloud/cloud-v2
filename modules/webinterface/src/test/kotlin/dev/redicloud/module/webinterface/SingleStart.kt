package dev.redicloud.module.webinterface

fun main() {
    val module = WebinterfaceModule()
    module.load()
    while (true) {
        val input = readlnOrNull() ?: break
        if (input == "stop" || input == "exit") {
            module.unload()
            break
        }
    }
}