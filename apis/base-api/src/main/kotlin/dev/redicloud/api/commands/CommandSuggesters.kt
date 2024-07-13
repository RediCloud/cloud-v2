package dev.redicloud.api.commands

/**
 * An empty suggester that returns an empty array. Used as default argument suggester.
 */
class EmptySuggester : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> = arrayOf()
}

/**
 * A suggester that returns all available memory values that are smaller than the total memory.
 */
class MemorySuggester : AbstractCommandSuggester() {
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

/**
 * A suggester that returns the two possible values for a boolean.
 */
class BooleanSuggester : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> = arrayOf("true", "false")
}

/**
 * A suggester that returns integers in a given range if provided. This is an example for annotated parameters.
 */
class IntegerSuggester : AbstractCommandSuggester() {
    override fun suggest(context: CommandContext): Array<String> {
        val min = context.getOr(0, 1)
        val max = context.getOr(1, 10)
        val step = context.getOr(2, 1)
        return (min..max step step).map { it.toString() }.toTypedArray()
    }
}