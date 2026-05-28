package dev.redicloud.testing.config

data class NodeConfig(
    var name: String,
    var startUpCommands: MutableList<String> = mutableListOf(),
    var shutdownCommands: MutableList<String> = mutableListOf(),
    var maxMemory: Int = 2048
)
