package dev.redicloud.commands.api

import dev.redicloud.api.commands.*


val SUGGESTERS = mutableListOf(
    EmptySuggester(),
    MemorySuggester(),
    IntegerSuggester(),
    BooleanSuggester()
)

class CommandSubPathSuggester(val subCommand: CommandSubBase) : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> = subCommand.getSubPaths().toTypedArray()
}

class CommandSuggester(val command: CommandBase) : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> {
        return if (context.input.isEmpty()) {
            arrayOf(command.name)
        }else {
            arrayOf(command.name, *command.aliases)
        }
    }
}

class CommandArgumentSuggester(val commandArgument: CommandArgument) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> {
        if (!commandArgument.subCommand.isThis(context.input, true)) return arrayOf()
        val nextArgument = commandArgument.isThis(context.input, true)
        if (nextArgument) {
            context.input.removeLastSpaces().removeFirstSpaces()
            return commandArgument.annotatedSuggester.preSuggest(context).map { it }.toTypedArray()
        }
        val input = context.input.removeLastSpaces().removeFirstSpaces()
        val lastArgument = input.split(" ").last()
        input.split(" ").dropLast(1).joinToString(" ")
        return commandArgument.annotatedSuggester.preSuggest(context)
            .filter { it.lowercase().startsWith(lastArgument.lowercase()) }
            .toTypedArray()
    }

}