package dev.redicloud.commands.api

import dev.redicloud.utils.EasyCache
import kotlin.time.Duration.Companion.seconds

abstract class AbstractCommandSuggester {
    companion object {
        val SUGGESTERS = mutableListOf(
            EmptySuggester(),
            MemorySuggester(),
            IntegerSuggester(),
            BooleanSuggester()
        )
    }

    private val cache: EasyCache<Array<String>, CommandContext> = EasyCache(5.seconds) {
        suggest(it!!)
    }

    fun preSuggest(context: CommandContext): Array<String> {
        return cache.get(context)!!
    }

    abstract fun suggest(context: CommandContext): Array<String>
}

class EmptySuggester() : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> = arrayOf()
}

class MemorySuggester() : AbstractCommandSuggester() {
    private val memoryList = mutableListOf<Long>(
        524,
        1024,
        2048,
        4096,
        8192,
        16384,
        32768,
        65536,
        131072,
        262144,
        524288,
        1048576
    )

    override fun suggest(context: CommandContext): Array<String> {
        val totalMemory = Runtime.getRuntime().totalMemory()
        return memoryList.filter { it < totalMemory }.map { it.toString() }.sortedDescending().toTypedArray()
    }
}

class BooleanSuggester() : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> = arrayOf("true", "false")
}

class IntegerSuggester() : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> {
        val min = context.getOr(0, 1)
        val max = context.getOr(1, 10)
        val step = context.getOr(2, 1)
        return (min..max step step).map { it.toString() }.toTypedArray()
    }
}

class CommandSubPathSuggester(val subCommand: CommandSubBase) : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> = subCommand.getSubPaths().toTypedArray()
}

class CommandSuggester(val command: CommandBase) : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> =
        arrayOf(command.getName(), *command.getAliases())
}

class CommandArgumentSuggester(val commandArgument: CommandArgument) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> {
        if (!commandArgument.isThis(context.input, true)) return arrayOf()
        val nextArgument = !commandArgument.isThis(context.input, false)
        if (nextArgument) {
            context.input.removeLastSpaces().removeFirstSpaces()
            return commandArgument.annotatedSuggester.preSuggest(context).map { it }.toTypedArray()
        }
        val input = context.input.removeLastSpaces().removeFirstSpaces()
        val lastArgument = input.split(" ").last()
        input.split(" ").dropLast(1).joinToString(" ")
        return commandArgument.annotatedSuggester.preSuggest(context)
            .filter { lastArgument.lowercase().startsWith(it.lowercase()) }
            .toTypedArray()
    }

}