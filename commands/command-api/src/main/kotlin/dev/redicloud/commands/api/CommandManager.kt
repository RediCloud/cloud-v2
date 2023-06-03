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

    fun handleInput(actor: ICommandActor<*>, input: String): CommandResponse {
        if (input.isBlank()) return CommandResponse(CommandResponseType.INVALID_COMMAND, "Command cannot be blank")
        val split = input.split(" ")
        val commandName = split[0].lowercase()
        val parameters = split.drop(1)
        val command = commands.firstOrNull { it.getName().lowercase() == commandName }
            ?: return CommandResponse(CommandResponseType.INVALID_COMMAND, "Command $commandName not found")

        if (!actor.hasPermission(command.getPermission())) return CommandResponse(CommandResponseType.PERMISSION, "You do not have permission to execute this command! ${command.getPermission()}")

        val possibleFullPaths = command.getPaths()
        val possiblePaths = command.getPathsWithArguments()

        val matched = possibleFullPaths.toMutableList()
        var index = -1
        parameters.forEach {
            index++
            val possible = matched.filter { path ->
                val parameterSplit = path.split(" ")
                if (parameterSplit.size <= index) return@filter false
                val parameter = parameterSplit[index].lowercase()
                parameter == it.lowercase() || parameter.isOptionalArgument() || parameter.isRequiredArgument()
            }
            matched.clear()
            matched.addAll(possible)
        }

        val subCommand = command.getSubCommands()
            .firstOrNull { subCommand -> subCommand.getSubPaths()
                .any { it -> it.lowercase() == matched.first().lowercase() } }
            ?: return CommandResponse(CommandResponseType.INVALID_SUB_PATH, "Invalid sub path for command $commandName ${parameters.joinToString(" ")}")

        if (!actor.hasPermission(subCommand.permission)) return CommandResponse(CommandResponseType.PERMISSION, "You do not have permission to execute this command! ${subCommand.permission}")

        val optimalPath = subCommand.getSubPaths().firstOrNull { it.lowercase() == matched.first().lowercase() }!!
        var pathWithoutArgs = ""
        optimalPath.split(" ").forEach {
            if (it.isOptionalArgument() || it.isRequiredArgument()) return@forEach
            if (pathWithoutArgs.isNotBlank()) pathWithoutArgs += " "
            pathWithoutArgs += it
        }
        val arguments = parameters.drop(pathWithoutArgs.split(" ").size)
        return subCommand.execute(actor, arguments)
    }

    fun getCommands(): List<CommandBase> = commands.toList()

}