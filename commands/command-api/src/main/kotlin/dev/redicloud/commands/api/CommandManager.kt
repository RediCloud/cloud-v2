package dev.redicloud.commands.api

import dev.redicloud.api.commands.*
import kotlin.reflect.KClass

abstract class CommandManager<K : ICommandActor<*>> : ICommandManager<K> {

    val registeredCommands = mutableListOf<CommandBase>()
    private val disabledCommands = mutableListOf<CommandBase>()
    private var allDisabled = false
    override var helpFormatter: ICommandHelpFormatter = DefaultHelpFormatter(this)

    override fun registerHelpCommand(): IRegisteredCommand {
        return registerCommand(HelpCommand(this))
    }

    override fun registerCommand(command: ICommand): IRegisteredCommand {
        val registeredCommand = CommandBase(command, this)
        registeredCommands.add(registeredCommand)
        return registeredCommand
    }

    override fun unregisterCommand(command: IRegisteredCommand) {
        registeredCommands.remove(command)
    }

    override fun disableCommands() {
        disabledCommands.addAll(registeredCommands)
        allDisabled = true
    }

    override fun enableCommands() {
        disabledCommands.clear()
        allDisabled = false
    }

    override fun isDisabled(command: IRegisteredCommand): Boolean {
        val commandBase = getInternal(command) ?: return false
        return disabledCommands.contains(command) || allDisabled
    }

    override fun disableCommand(command: IRegisteredCommand) {
        val commandBase = getInternal(command) ?: return
        disabledCommands.add(commandBase)
    }

    override fun enableCommand(command: IRegisteredCommand) {
        val commandBase = getInternal(command) ?: return
        disabledCommands.remove(commandBase)
        allDisabled = false
    }

    override fun isCommandRegistered(command: ICommand): Boolean {
        return getInternal(command) != null
    }

    override fun <T : Any> registerParser(clazz: KClass<T>, parser: ICommandArgumentParser<T>) {
        PARSERS[clazz] = parser
    }

    override fun registerSuggesters(vararg suggesters: AbstractCommandSuggester) {
        SUGGESTERS.addAll(suggesters)
    }

    override fun areCommandsDisabled(): Boolean = allDisabled

    override fun unregisterParser(parser: ICommandArgumentParser<*>) {
        var toRemove: KClass<*>? = null
        PARSERS.forEach {
            if (it.value != parser) return@forEach
            toRemove = it.key
        }
        toRemove?.let { PARSERS.remove(it) }
    }

    override fun unregisterSuggester(suggester: AbstractCommandSuggester) {
        SUGGESTERS.remove(suggester)
    }

    fun getCommand(input: String): CommandBase? = registeredCommands.firstOrNull { it.isThis(input, false) }

    fun getCompletions(actor: K, input: String): List<String> {
        val list = mutableListOf<String>()

        val split = input.removeLastSpaces().split(" ")
        if (split.isEmpty()) return list
        val parameters = split.drop(1)
        val commandName = split[0]
        val commandBase = getCommand(input)
        val possibleCommands = registeredCommands
            .filter { actor.hasPermission(it.permission) }
            .filter { !isDisabled(it) }
            .filter { it.isThis(input, commandBase == null) }

        if (possibleCommands.isEmpty()) return list

        possibleCommands.forEach { command ->
            val possibleSubCommands = command.subCommands
                .filter { it.isThis(input, true) }
                .filter { actor.hasPermission(it.permission) }
                if (possibleSubCommands.size == 1) {
                list.addAll(possibleSubCommands.first().suggester.preSuggest(CommandContext(input, emptyArray())))
                possibleSubCommands.first().arguments
                    .filter { it.isThis(input, true) }
                    .map { it.subCommand.suggester.preSuggest(CommandContext(input, it.annotatedSuggesterParameter)) }
            }else{
                possibleSubCommands.forEach {
                    list.addAll(it.getSubPaths())
                }
            }
        }

        list.removeIf { it.isBlank() }

        if ((list.isEmpty() || commandBase == null)
            && possibleCommands.isNotEmpty()
            && input.removeLastSpaces().split(" ").size == 1) {
            list.clear()
            list.addAll(possibleCommands
                .filter { actor.hasPermission(it.permission) }
                .filter { !isDisabled(it) }
                .flatMap {
                    if (input.isBlank()) {
                        mutableListOf(it.name)
                    }else {
                        mutableListOf(it.name, *it.aliases)
                    }
                }
                .filter { it.lowercase().startsWith(split[0].lowercase()) })
            return list
        }

        val results = mutableSetOf<String>()

        val currentIndex = if (input.endsWith(" ")) split.size else split.size - 1
        list.forEach { path ->
            val split1 = "$commandName $path".removeLastSpaces().split(" ")
            if (split1.size < currentIndex+1) return@forEach
            val result = split1[currentIndex]
            val inputR = if (currentIndex-1 == -1) commandName else if (parameters.size < currentIndex+1) "" else parameters[currentIndex]
            if (commandBase != null
                && listOf(commandBase.name, *commandBase.aliases).any { inputR.lowercase() == it.lowercase() }
                && !result.isArgument()
                && result.isNotBlank()
                && split1.size > currentIndex + 1) {
                results.add(split1[currentIndex + 1])
            }else if(inputR.isEmpty() && !result.isArgument()) {
                results.add(split1[currentIndex])
            }else if (!input.endsWith(" ")
                && result.lowercase().startsWith(inputR.lowercase())
                && !result.isArgument()
                && result.isNotBlank()) {
                results.add(result)
            }else {
                if (split1.size >= currentIndex + 2 && inputR != "") return@forEach
                val nextResult = if (inputR == "") result else split1[currentIndex + 1]
                if (nextResult.isArgument()) {
                    commandBase?.subCommands?.filter { it.isThis(input, true) }?.forEach { subCommand ->
                        subCommand.arguments.filter { it.isThis(input, true) }.forEach { argument ->
                            results.addAll(argument.suggester.preSuggest(CommandContext(input, argument.annotatedSuggesterParameter)))
                        }
                    }
                }
            }
        }
        results.removeIf { it.isBlank() }
        return results.toList()
    }

    fun handleInput(actor: K, input: String): CommandResponse {
        if (input.isBlank()) return CommandResponse(CommandResponseType.BLANK_INPUT, "Command cannot be blank")
        val split = input.removeLastSpaces().split(" ")
        val commandName = split[0].lowercase()
        val parameters = split.drop(1)
        val command = getCommand(commandName)
            ?: return helpFormatter.formatHelp(actor, CommandContext(input, emptyArray()))
        if (isDisabled(command)) return CommandResponse(CommandResponseType.DISABLED, "Command '$commandName' is disabled")

        if (!actor.hasPermission(command.permission)) return CommandResponse(
            CommandResponseType.PERMISSION,
            "You do not have permission to execute this command! (${command.permission})"
        )

        val subCommand = command.subCommands
            .firstOrNull { it.isThis(input, false) } ?: return helpFormatter.formatHelp(actor, CommandContext(input, emptyArray()))

        if (!actor.hasPermission(subCommand.permission)) return CommandResponse(
            CommandResponseType.PERMISSION,
            "You do not have permission to execute this command! (${subCommand.permission})"
        )

        val optimalPath = subCommand.parseToOptimalPath(input)!!
        val argumentIndexes = mutableListOf<Int>()
        var varargStart = -1
        var index = -1
        optimalPath.split(" ").forEach {
            index++
            if (it.isVarArgument()) {
                varargStart = index
                return@forEach
            }
            if (it.isOptionalArgument() || it.isRequiredArgument()) argumentIndexes.add(index)
        }
        val vararg = subCommand.arguments.lastOrNull { !it.actorArgument }?.vararg == true
        val arguments = parameters.filterIndexed { i, _ -> argumentIndexes.contains(i) }.toMutableList()
        if (vararg) {
            val varargArguments = parameters.filterIndexed { i, _ -> i >= varargStart }
            arguments.addAll(varargArguments)
        }
        return subCommand.execute(actor, arguments)
    }

    fun helpPaths(commandBase: CommandBase, parameterInput: String): List<CommandSubBase> {
        val instantResult = commandBase.getSubCommand(parameterInput)
        if (instantResult != null) return listOf(instantResult)
        val results = commandBase.subCommands.toMutableList()
        var storedLastResults = results.toList()
        var currentInput = parameterInput.removeFirstSpaces()
        while ((storedLastResults.size > 2 && results.isNotEmpty())) {
            if (results.isNotEmpty()) storedLastResults = results.toList()
            results.clear()
            commandBase.subCommands.forEach {
                if (it.isThis("${commandBase.name} $currentInput", true)) {
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

    fun getInternal(command: Any): CommandBase? {
        return when (command) {
            is CommandBase -> command
            is IRegisteredCommand -> registeredCommands.firstOrNull { it as IRegisteredCommand == command }
            is ICommand -> registeredCommands.firstOrNull { it.commandImpl == command }
            else -> throw IllegalArgumentException("Command must be a CommandBase, IRegisteredCommand or ICommand (got ${command::class.simpleName})")
        }
    }

    override fun getCommands(): List<IRegisteredCommand> {
        return registeredCommands
    }

}