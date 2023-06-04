package dev.redicloud.console.commands

import dev.redicloud.commands.api.ICommandActor
import dev.redicloud.console.Console
import java.util.UUID

class ConsoleActor(val console: Console, override val identifier: UUID) : ICommandActor<UUID> {

    override fun hasPermission(permission: String?): Boolean = true
    override fun sendMessage(message: String) = console.print {
        prefix("COMMAND")
        text(message)
    }

}