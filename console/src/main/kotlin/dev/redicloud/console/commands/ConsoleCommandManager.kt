package dev.redicloud.console.commands

import dev.redicloud.commands.api.CommandManager
import dev.redicloud.console.Console
import java.util.*

class ConsoleCommandManager(val console: Console) : CommandManager<ConsoleActor>() {

    val actor = ConsoleActor(console, UUID.randomUUID())

    override fun getActor(identifier: ConsoleActor): ConsoleActor = actor


}