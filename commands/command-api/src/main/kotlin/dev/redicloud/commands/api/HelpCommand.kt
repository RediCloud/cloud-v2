package dev.redicloud.commands.api

import dev.redicloud.api.commands.*

@Command("help")
@CommandAlias(["h", "?"])
@CommandDescription("Shows all available commands")
class HelpCommand(val commandManager: CommandManager<*>) : ICommand {

    @CommandSubPath("")
    fun help(
        actor: ICommandActor<*>
    ) {
        val commands = this.commandManager.getCommands()
        if (commands.isEmpty()) {
            actor.sendMessage("There are no commands available!")
            return
        }
        actor.sendHeader("Help")
        actor.sendMessage("")
        commands.forEach {
            actor.sendMessage("%hc%${it.name} §8| %tc%Aliases: %hc%${it.aliases.joinToString(", ")}")
            actor.sendMessage("§8➥ %tc%${it.description}")
            actor.sendMessage("")
        }
        actor.sendHeader("Help")
    }

}