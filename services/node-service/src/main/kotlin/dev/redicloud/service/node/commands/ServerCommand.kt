package dev.redicloud.service.node.commands

import dev.redicloud.commands.api.Command
import dev.redicloud.commands.api.CommandAlias
import dev.redicloud.commands.api.CommandDescription

@Command("server")
@CommandAlias(["s", "servers"])
@CommandDescription("Manage the servers")
class ServerCommand {

}