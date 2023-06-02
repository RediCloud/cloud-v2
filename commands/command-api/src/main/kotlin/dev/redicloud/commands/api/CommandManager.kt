package dev.redicloud.commands.api

open class CommandManager {

    private val commands = mutableListOf<CommandBase>()
    fun register(command: CommandBase) {
        command.load()
        commands.add(command)
    }

    fun unregister(command: CommandBase) {
        commands.remove(command)
    }

    fun handleInput(input: String): CommandResponse {
        val split = input.split(" ")
        val commandName = split[0].lowercase()
        val command = commands.firstOrNull { it.getName().lowercase() == commandName }
            ?: return CommandResponse(CommandResponseType.INVALID_COMMAND, "Command $commandName not found")
        val subCommand = command.getSubCommands().firstOrNull { it.getSubPathsWithoutArguments().contains(split.joinToString(" ")) }
            ?: return CommandResponse(CommandResponseType.INVALID_SUB_PATH, "Sub command ${split.joinToString(" ")} not found")
        val possiblePaths = subCommand.getSubPaths()
        val path = possiblePaths.firstOrNull { it == split.joinToString(" ") }
            ?: return CommandResponse(CommandResponseType.INVALID_SUB_PATH, "Path ${split.joinToString(" ")} not found")
        return CommandResponse(CommandResponseType.SUCCESS, "Path $path found")
    }

    fun getCommands(): List<CommandBase> = commands.toList()

}