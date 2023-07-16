package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.commands.api.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.utils.BUILD_NUMBER
import dev.redicloud.utils.CLOUD_VERSION
import dev.redicloud.utils.GIT

@Command("version")
@CommandAlias(["ver"])
@CommandDescription("Displays the current version of the node service")
class VersionCommand : ICommand {

    @CommandSubPath("")
    @CommandDescription("Displays the current version of the node service")
    fun version(
        actor: ConsoleActor
    ) {
        actor.sendHeader("Version")
        actor.sendMessage("Version§8: %hc%${CLOUD_VERSION}")
        actor.sendMessage("Git§8: %hc%${GIT}")
        actor.sendMessage("CI-Build§8: %hc%${BUILD_NUMBER}")
        actor.sendHeader("Version")
    }

}