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

    fun getCommand(input: String): CommandBase? = commands.firstOrNull { it.isThis(input, false) }

    //TODO
    fun getCompletions(actor: K, input: String): List<String> {
        val list = mutableListOf<String>()

        val split = input.split(" ")
        if (split.isEmpty()) return list
        val commandName = split[0].lowercase()
        val parameters = split.drop(1)
        val command = getCommand(commandName) ?: return list
        if (!actor.hasPermission(command.getPermission())) return list
        list.addAll(command.suggester.suggest(CommandSuggesterContext(input, emptyArray())))

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
            list.addAll(command.getSubCommands().filter { actor.hasPermission(it.permission) }
                .flatMap { it.suggester.suggest(CommandSuggesterContext(input, emptyArray())).toList() })
            return list
        }

        list.addAll(subCommand.arguments.flatMap { it.suggester.suggest(CommandSuggesterContext(input, emptyArray())).toList() })

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

        val subCommand = command.getSubCommands()
            .firstOrNull { it.isThis(input, false) }
            ?: return CommandResponse(CommandResponseType.INVALID_SUB_PATH, "Invalid sub path for command $commandName ${parameters.joinToString(" ")}")

        if (!actor.hasPermission(subCommand.permission)) return CommandResponse(CommandResponseType.PERMISSION, "You do not have permission to execute this command! ${subCommand.permission}")

        val optimalPath = subCommand.parseToOptimalPath(input)!!
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