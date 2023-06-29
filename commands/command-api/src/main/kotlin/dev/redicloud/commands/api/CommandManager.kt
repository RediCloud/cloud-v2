package dev.redicloud.commands.api

abstract class CommandManager<K : ICommandActor<*>> {

    private val commands = mutableListOf<CommandBase>()
    private val disabledCommands = mutableListOf<CommandBase>()
    private var allDisabled = false
    var helpFormatter: ICommandHelpFormatter = DefaultHelpFormatter(this)

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
            list.addAll(possibleCommands.first().suggester.suggest(CommandContext(input, emptyArray())))
        }

        possibleCommands.forEach { command ->
            val possibleSubCommands = command.getSubCommands()
                .filter { it.isThis(input, true) }
                .filter { actor.hasPermission(it.permission) }

            if (possibleSubCommands.size == 1) {
                list.addAll(possibleSubCommands.first().suggester.suggest(CommandContext(input, emptyArray())))
                possibleSubCommands.first().arguments
                    .filter { it.isThis(input, true) }
                    .map { it.subCommand.suggester.suggest(CommandContext(input, it.suggesterParameter)) }
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
            ?: return helpFormatter.formatHelp(actor, CommandContext(input, emptyArray()))
        if (isDisabled(command)) return CommandResponse(CommandResponseType.DISABLED, "Command '$commandName' is disabled")

        if (!actor.hasPermission(command.getPermission())) return CommandResponse(
            CommandResponseType.PERMISSION,
            "You do not have permission to execute this command! (${command.getPermission()})"
        )

        val subCommand = command.getSubCommands()
            .firstOrNull { it.isThis(input, false) } ?: return helpFormatter.formatHelp(actor, CommandContext(input, emptyArray()))

        if (!actor.hasPermission(subCommand.permission)) return CommandResponse(
            CommandResponseType.PERMISSION,
            "You do not have permission to execute this command! (${subCommand.permission})"
        )

        val optimalPath = subCommand.parseToOptimalPath(input)!!
        val argumentIndexes = mutableListOf<Int>()
        var index = -1
        optimalPath.split(" ").forEach {
            index++
            if (it.isOptionalArgument() || it.isRequiredArgument()) argumentIndexes.add(index)
        }
        val arguments = parameters.filterIndexed { i, _ -> argumentIndexes.contains(i) }
        return subCommand.execute(actor, arguments)
    }

    fun helpPaths(commandBase: CommandBase, parameterInput: String): List<CommandSubBase> {
        val instantResult = commandBase.getSubCommand(parameterInput)
        if (instantResult != null) return listOf(instantResult)
        val results = commandBase.getSubCommands().toMutableList()
        var storedLastResults = results.toList()
        var currentInput = parameterInput.removeFirstSpaces()
        while ((storedLastResults.size > 2 && results.isNotEmpty())) {
            if (results.isNotEmpty()) storedLastResults = results.toList()
            results.clear()
            commandBase.getSubCommands().forEach {
                if (it.isThis("${commandBase.getName()} $currentInput", true)) {
                    results.add(it)
                }else {
                    results.remove(it)
                }
            }
            val split = currentInput.removeLastSpaces().split(" ").toMutableList()
            if (split.isEmpty()) return if (results.isEmpty()) storedLastResults else results
            split.removeLast()
            if (split.isEmpty()) return if (results.isEmpty()) storedLastResults else results
            currentInput = split.joinToString(separator = " ")
        }

        return if (results.isEmpty()) storedLastResults else results
    }

    fun getCommands(): List<CommandBase> = commands.toList()

}