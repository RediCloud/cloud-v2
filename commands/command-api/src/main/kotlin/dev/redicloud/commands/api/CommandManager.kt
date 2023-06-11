package dev.redicloud.commands.api

abstract class CommandManager<K : ICommandActor<*>> {

    private val commands = mutableListOf<CommandBase>()
    private val disabledCommands = mutableListOf<CommandBase>()
    private var allDisabled = false

    abstract fun getActor(identifier: K): K

    fun register(command: CommandBase) {
        command.load()
        commands.add(command)
    }

    fun unregister(command: CommandBase) {
        commands.remove(command)
    }

    fun disableCommands() {
        disabledCommands.addAll(commands)
        allDisabled = true
    }

    fun enableCommands() {
        disabledCommands.clear()
        allDisabled = false
    }

    fun isDisabled(command: CommandBase): Boolean = disabledCommands.contains(command) || allDisabled

    fun disableCommand(command: CommandBase) {
        disabledCommands.add(command)
    }

    fun enableCommand(command: CommandBase) {
        disabledCommands.remove(command)
        allDisabled = false
    }

    fun areCommandsDisabled(): Boolean = allDisabled

    fun getCommand(input: String): CommandBase? = commands.firstOrNull { it.isThis(input, false) }

    fun getCompletions(actor: K, input: String): List<String> {
        val list = mutableListOf<String>()

        val split = input.split(" ")
        if (split.isEmpty()) return list
        val possibleCommands = commands
            .filter { actor.hasPermission(it.getPermission()) }
            .filter { !isDisabled(it) }
            .filter { it.isThis(input, true) }

        if (possibleCommands.isEmpty()) return list

        if (possibleCommands.size == 1) {
            list.addAll(possibleCommands.first().suggester.suggest(CommandSuggesterContext(input, emptyArray())))
        }

        possibleCommands.forEach { command ->
            val possibleSubCommands = command.getSubCommands()
                .filter { it.isThis(input, true) }
                .filter { actor.hasPermission(it.permission) }

            if (possibleSubCommands.size == 1) {
                list.addAll(possibleSubCommands.first().suggester.suggest(CommandSuggesterContext(input, emptyArray())))
                possibleSubCommands.first().arguments
                    .filter { it.isThis(input, true) }
                    .map { it.subCommand.suggester.suggest(CommandSuggesterContext(input, it.suggesterParameter)) }
            }

        }
        return list
    }

    fun handleInput(actor: K, input: String): CommandResponse {
        if (input.isBlank()) return CommandResponse(CommandResponseType.BLANK_INPUT, "Command cannot be blank")
        val split = input.removeLastSpaces().split(" ")
        val commandName = split[0].lowercase()
        val parameters = split.drop(1)
        val command = getCommand(commandName)
            ?: return CommandResponse(CommandResponseType.INVALID_COMMAND, "Command '$commandName' not found")
        if (isDisabled(command)) return CommandResponse(CommandResponseType.DISABLED, "Command '$commandName' is disabled")

        if (!actor.hasPermission(command.getPermission())) return CommandResponse(
            CommandResponseType.PERMISSION,
            "You do not have permission to execute this command! (${command.getPermission()})"
        )

        val subCommand = command.getSubCommands()
            .firstOrNull { it.isThis(input, false) }
            ?: return CommandResponse(
                CommandResponseType.INVALID_SUB_PATH,
                "Invalid sub path for command '$commandName${if(parameters.isEmpty()) "" else (" ${parameters.joinToString(" ")}").removeLastSpaces()}'"
            )

        if (!actor.hasPermission(subCommand.permission)) return CommandResponse(
            CommandResponseType.PERMISSION,
            "You do not have permission to execute this command! (${subCommand.permission})"
        )

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