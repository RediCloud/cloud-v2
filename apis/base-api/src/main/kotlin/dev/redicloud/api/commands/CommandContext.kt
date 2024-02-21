package dev.redicloud.api.commands

/**
 * Represents the context of a command.
 * @param input The complete input of the command.
 * @param annotationArguments Arguments provided by the command parameter annotation.
 */
class CommandContext(val input: String, val annotationArguments: Array<String>) {

    /**
     * Gets an annotation argument by index or returns the default value if the index is out of bounds.
     */
    fun <T> getOr(index: Int, default: T): T =
        if (annotationArguments.size > index) annotationArguments[index] as T else default

    override fun equals(other: Any?): Boolean {
        if (other !is CommandContext) return false

        if (input != other.input) return false
        return annotationArguments.contentEquals(other.annotationArguments)
    }

    override fun hashCode(): Int {
        var result = input.hashCode()
        result = 31 * result + annotationArguments.contentHashCode()
        return result
    }

}