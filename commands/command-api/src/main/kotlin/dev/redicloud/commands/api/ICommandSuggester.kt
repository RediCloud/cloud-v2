package dev.redicloud.commands.api

import dev.redicloud.utils.EasyCache
import kotlin.time.Duration.Companion.seconds

interface ICommandSuggester {
    companion object {
        val SUGGESTERS = mutableListOf(
            EmptySuggester(),
            MemorySuggester(),
            IntegerSuggester(),
            BooleanSuggester()
        )
    }

    fun suggest(context: CommandContext): Array<String>
}

class EmptySuggester() : ICommandSuggester {
    override fun suggest(context: CommandContext): Array<String> = arrayOf()
}

class MemorySuggester() : ICommandSuggester {
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
        return memoryList.filter { it.toLong() < totalMemory }.map { it.toString() }.toTypedArray()
    }
}

class BooleanSuggester() : ICommandSuggester {
    override fun suggest(context: CommandContext): Array<String> = arrayOf("true", "false")
}

class IntegerSuggester() : ICommandSuggester {
    override fun suggest(context: CommandContext): Array<String> {
        val min = context.getOr(0, 1)
        val max = context.getOr(1, 10)
        val step = context.getOr(2, 1)
        return (min..max).map { it.toString() }.toTypedArray()
    }
}

class CommandSubPathSuggester(val subCommand: CommandSubBase) : ICommandSuggester {
    private val easyCache = EasyCache<Array<String>, CommandContext>(5.seconds) { context ->
        subCommand.getSubPaths().toTypedArray()
    }
    override fun suggest(context: CommandContext): Array<String> =
        easyCache.get(context)!!
}

class CommandSuggester(val command: CommandBase) : ICommandSuggester {
    override fun suggest(context: CommandContext): Array<String> =
        arrayOf(command.getName(), *command.getAliases())
}

class CommandArgumentSuggester(val commandArgument: CommandArgument) : ICommandSuggester {

    private val easyCache = EasyCache<Array<String>, CommandContext>(5.seconds) { context ->
        if (context == null) return@EasyCache arrayOf()
        if (!commandArgument.isThis(context.input, true)) return@EasyCache arrayOf()
        val nextArgument = !commandArgument.isThis(context.input, false)
        if (nextArgument) {
            val input = context.input.removeLastSpaces().removeFirstSpaces()
            return@EasyCache commandArgument.annotatedSuggester.suggest(context).map { it }.toTypedArray()
        }
        val input = context.input.removeLastSpaces().removeFirstSpaces()
        val lastArgument = input.split(" ").last()
        val currentPathWithoutArgument = input.split(" ").dropLast(1).joinToString(" ")
        return@EasyCache commandArgument.annotatedSuggester.suggest(context)
            .filter { lastArgument.lowercase().startsWith(it.lowercase()) }
            .toTypedArray()
    }

    override fun suggest(context: CommandContext): Array<String> = easyCache.get(context)!!

}