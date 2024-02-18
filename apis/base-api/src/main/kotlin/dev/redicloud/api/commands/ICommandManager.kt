package dev.redicloud.api.commands

import kotlin.reflect.KClass

interface ICommandManager<K : ICommandActor<*>> {

    var helpFormatter: ICommandHelpFormatter

    fun registerHelpCommand(): IRegisteredCommand

    fun registerCommand(command: ICommand): IRegisteredCommand

    fun unregisterCommand(command: IRegisteredCommand)

    fun isCommandRegistered(command: ICommand): Boolean

    fun enableCommands()

    fun disableCommands()

    fun isDisabled(command: IRegisteredCommand): Boolean

    fun enableCommand(command: IRegisteredCommand)

    fun disableCommand(command: IRegisteredCommand)

    fun areCommandsDisabled(): Boolean

    fun areCommandsEnabled(): Boolean = !areCommandsDisabled()

    fun getCommands(): List<IRegisteredCommand>

    fun registerSuggesters(vararg suggesters: AbstractCommandSuggester)

    fun unregisterSuggester(suggester: AbstractCommandSuggester)

    fun <T : Any> registerParser(clazz: Class<T>, parser: ICommandArgumentParser<T>) {
        registerParser(clazz.kotlin, parser)
    }

    fun <T : Any> registerParser(clazz: KClass<T>, parser: ICommandArgumentParser<T>)

    fun unregisterParser(parser: ICommandArgumentParser<*>)

    fun getActor(identifier: Any): K

}