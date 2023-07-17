package dev.redicloud.api.commands

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