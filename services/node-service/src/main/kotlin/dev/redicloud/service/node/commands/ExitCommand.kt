package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.service.node.NodeService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Command("exit")
@CommandAlias(["stop", "quit"])
@CommandDescription("Stops the node service")
class ExitCommand(private val nodeService: NodeService) : ICommand {

    companion object {
        private val EXIT_CONFIRM_TIMEOUT_MS = 10000.milliseconds
    }

    private var confirmed = System.getProperty("redicloud.exit.confirm", "false").toBoolean()

    @CommandSubPath("")
    fun onStop(actor: ConsoleActor) {
        if (!confirmed) {
            actor.sendMessage("§cTo shutdown the node enter the command again within 10 seconds!")
            confirmed = true
            nodeService.scope.launch {
                delay(EXIT_CONFIRM_TIMEOUT_MS)
                confirmed = false
            }
            return
        }
        nodeService.shutdown()
    }

    @CommandSubPath("--force")
    fun onForceStop(actor: ConsoleActor) {
        if (!confirmed) {
            actor.sendMessage("§cTo shutdown the node enter the command again within 10 seconds!")
            confirmed = true
            nodeService.scope.launch {
                delay(EXIT_CONFIRM_TIMEOUT_MS)
                confirmed = false
            }
            return
        }
        nodeService.shutdown(true)
    }
}
