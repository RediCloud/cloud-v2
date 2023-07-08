package dev.redicloud.commands.api

class CommandContext(val input: String, val annotationArguments: Array<String>) {
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