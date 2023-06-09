package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.service.node.NodeService
import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Command("exit")
@CommandAlias(["stop", "quit"])
@CommandDescription("Stops the node service")
class ExitCommand : CommandBase() {

    private var confirmed = System.getProperty("redicloud.exit.confirm", "false").toBoolean()

    @CommandSubPath("")
    fun onStop(actor: ConsoleActor) {
        if (!confirmed) {
            actor.sendMessage("§cTo shutdown the node enter the command again within 10 seconds!")
            confirmed = true
            defaultScope.launch {
                delay(10000)
                confirmed = false
            }
            return
        }
        NodeService.INSTANCE.shutdown()
    }

}