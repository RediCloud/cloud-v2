package dev.redicloud.commands.api

interface ICommandSuggester {
    companion object {
        val SUGGESTERS = mutableListOf(
            EmptySuggester(),
            MemorySuggester(),
            IntegerSuggester()
        )
    }

    fun suggest(context: CommandSuggesterContext): Array<String>
}

class CommandSuggesterContext(val input: String, val annotationArguments: Array<String>) {
    fun <T> getOr(index: Int, default: T): T =
        if (annotationArguments.size > index) annotationArguments[index] as T else default
}

class EmptySuggester() : ICommandSuggester {
    override fun suggest(context: CommandSuggesterContext): Array<String> = arrayOf()
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

    override fun suggest(context: CommandSuggesterContext): Array<String> {
        val totalMemory = Runtime.getRuntime().totalMemory()
        return memoryList.filter { it.toLong() < totalMemory }.map { it.toString() }.toTypedArray()
    }
}

class IntegerSuggester() : ICommandSuggester {
    override fun suggest(context: CommandSuggesterContext): Array<String> {
        val min = context.getOr(0, 1)
        val max = context.getOr(1, 10)
        val step = context.getOr(2, 1)
        return (min..max).map { it.toString() }.toTypedArray()
    }
}

class CommandSubPathSuggester(val subCommand: CommandSubBase) : ICommandSuggester {
    override fun suggest(context: CommandSuggesterContext): Array<String> =
        subCommand.getSubPathsWithoutArguments().toTypedArray()
}

class CommandSuggester(val command: CommandBase) : ICommandSuggester {
    override fun suggest(context: CommandSuggesterContext): Array<String> =
        arrayOf(command.getName(), *command.getAliases())
}
