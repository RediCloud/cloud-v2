package dev.redicloud.commands.api

import dev.redicloud.api.commands.*

class DefaultHelpFormatter(private val commandManager: CommandManager<*>) : ICommandHelpFormatter {

    override fun formatHelp(actor: ICommandActor<*>, context: CommandContext): CommandResponse {
        val split = context.input.removeLastSpaces().split(" ")
        val commandName = if (split.isEmpty()) "" else split[0].lowercase()
        val parameters = split.drop(1)

        val command = commandManager.registeredCommands.firstOrNull { it.isThis(context.input, false) }
        if (command == null) {
            val predictedCommands = commandManager.registeredCommands
                .filter { it.isThis(context.input, true) }
                .filter { actor.hasPermission(it.permission) }
            if (predictedCommands.isEmpty()) return CommandResponse(
                CommandResponseType.INVALID_COMMAND,
                "Command '$commandName' was not found!"
            )
            actor.sendHeader("Help")
            actor.sendMessage("")
            actor.sendMessage("§cCommand was not found! %tc%Here are some suggestions")
            actor.sendMessage("")
            predictedCommands.forEach {
                actor.sendMessage("%hc%${it.name} §8| %tc%${it.aliases.joinToString(separator = "§8, %tc%")}")
                actor.sendMessage("§8➥ %tc%${it.description}")
                actor.sendMessage("")
            }
            actor.sendHeader("Help")
            return CommandResponse(
                CommandResponseType.HELP_SENT,
                "Help for '${context.input}' was sent!"
            )
        }

        val helpPaths = commandManager.helpPaths(command, parameters.joinToString(separator = " "))

        if (helpPaths.isNotEmpty()) {
            if (helpPaths.size == 1) {
                val subBase = helpPaths[0]
                actor.sendMessage("Usage: %hc%${subBase.getUsage()}")
                return CommandResponse(
                    CommandResponseType.HELP_SENT,
                    "Help for '${subBase.getUsage()}' was sent!"
                )
            }
            actor.sendHeader("Help")
            actor.sendMessage("")
            actor.sendMessage("§cSub command was not found! %tc%Here are some suggestions:")
            actor.sendMessage("")
            helpPaths.forEach {
                actor.sendMessage("%hc%${it.getUsage()}")
                actor.sendMessage("§8➥ %tc%${it.description}")
                actor.sendMessage("")
            }
            actor.sendHeader("Help")
            return CommandResponse(
                CommandResponseType.HELP_SENT,
                "Help for '${context.input}' was sent!"
            )
        }

        return CommandResponse(
            CommandResponseType.INVALID_SUB_PATH,
            "Invalid sub path for command '$commandName${
                if (parameters.isEmpty()) "" else (" ${
                    parameters.joinToString(
                        " "
                    )
                }").removeLastSpaces()
            }'"
        )
    }

}