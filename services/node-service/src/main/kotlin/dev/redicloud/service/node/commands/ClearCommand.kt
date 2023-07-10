package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.Console
import dev.redicloud.console.commands.ConsoleActor

@Command("clear")
@CommandAlias(["cc", "cl", "clearscreen", "cs"])
@CommandDescription("Clears the console")
class ClearCommand(private val console: Console) : CommandBase() {

    @CommandSubPath("")
    fun clear(
        actor: ConsoleActor
    ) {
        this.console.clearScreen()
        this.console.getScreens().filter { it.isDefault() }.forEach {
            it.clear()
        }
        actor.sendMessage("Your console has been cleared!")
    }

}
