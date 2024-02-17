package dev.redicloud.console.commands

import dev.redicloud.commands.api.CommandManager
import dev.redicloud.console.Console
import java.util.*

class ConsoleCommandManager(val console: Console) : CommandManager<ConsoleActor>() {

    init {
        this.registerHelpCommand()
    }

    val defaultActor = ConsoleActor(console, UUID.randomUUID())

    override fun getActor(identifier: Any): ConsoleActor = defaultActor

}