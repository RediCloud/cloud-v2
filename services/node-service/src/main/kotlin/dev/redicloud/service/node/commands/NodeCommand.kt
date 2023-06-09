package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.Command
import dev.redicloud.commands.api.CommandBase
import dev.redicloud.commands.api.CommandDescription
import dev.redicloud.commands.api.CommandSubPath
import dev.redicloud.console.commands.ConsoleActor

@Command("node")
@CommandDescription("All commands related to nodes")
class NodeCommand : CommandBase() {

    @CommandSubPath("list")
    @CommandDescription("List all nodes")
    fun list(actor: ConsoleActor) {

    }

}