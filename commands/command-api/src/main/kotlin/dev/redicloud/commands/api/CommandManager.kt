package dev.redicloud.commands.api

abstract class CommandManager<K : ICommandActor<*>> {

    private val commands = mutableListOf<CommandBase>()

    abstract fun getActor(identifier: K): K

    fun register(command: CommandBase) {
        command.load()
        commands.add(command)
    }

    fun unregister(command: CommandBase) {
        commands.remove(command)
    }

    fun getCommand(input: String): CommandBase? {
        val split = input.split(" ")
        if (split.isEmpty()) return null
        return commands.firstOrNull { it.getName().lowercase() == split[0].lowercase() }
    }

    fun getSuggester(actor: K, input: String): List<ICommandSuggester> {
        val list = mutableListOf<ICommandSuggester>()

        val split = input.split(" ")
        if (split.isEmpty()) return list
        val commandName = split[0].lowercase()
        val parameters = split.drop(1)
        val command = getCommand(commandName) ?: return list
        if (!actor.hasPermission(command.getPermission())) return list
        list.add(command.suggester)

        val possibleFullPaths = command.getPaths()

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
        if (subCommand == null || !actor.hasPermission(subCommand.permission)) {
            list.addAll(command.getSubCommands().filter { actor.hasPermission(it.permission) }.map { it.suggester })
            return list
        }

        list.add(subCommand.suggester)
        list.addAll(subCommand.arguments.map { it.suggester }.toList())

        return list
    }

    fun handleInput(actor: K, input: String): CommandResponse {
        if (input.isBlank()) return CommandResponse(CommandResponseType.INVALID_COMMAND, "Command cannot be blank")
        val split = input.split(" ")
        val commandName = split[0].lowercase()
        val parameters = split.drop(1)
        val command = getCommand(commandName)
            ?: return CommandResponse(CommandResponseType.INVALID_COMMAND, "Command $commandName not found")

        if (!actor.hasPermission(command.getPermission())) return CommandResponse(CommandResponseType.PERMISSION, "You do not have permission to execute this command! ${command.getPermission()}")

        val possibleFullPaths = command.getPaths()

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