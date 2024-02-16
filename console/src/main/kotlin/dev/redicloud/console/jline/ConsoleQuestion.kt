package dev.redicloud.console.jline

import dev.redicloud.commands.api.PARSERS
import dev.redicloud.console.Console

class ConsoleQuestion(
    val question: String,
    val condition: ConsoleQuestionCondition? = null,
    val default: Any? = null,
    val linePrefix: String = "§8» §f",
    val completer: List<String> = mutableListOf()
) {

    inline suspend fun <reified T> ask(console: Console): T {
        console.disableCommands()
        var result: T? = null
        while (result == null) {
            question.split("\n").forEach {
                console.writeLine("$linePrefix $it")
            }
            val input = console.readNextInput()
            if (input.isEmpty()) {
                if (default == null) {
                    console.writeLine("$linePrefix §cThere is no default value! Please enter a value!")
                    continue
                }
                return default as T
            }
            val fail = condition?.fail(input) ?: false
            if (fail) continue
            val parser = PARSERS[T::class]
                ?: throw IllegalArgumentException("No parser found for class ${T::class.simpleName}")
            try {
                result = parser.parse(input) as T?
            } catch (e: Exception) {
                console.writeLine("$linePrefix §cError while parsing input! Please try again!")
            }
        }
        console.enableCommands()
        return result
    }

}

interface ConsoleQuestionCondition {
    fun fail(input: String): Boolean
}

inline suspend fun <reified T> Console.ask(
    question: String,
    condition: ConsoleQuestionCondition? = null,
    default: Any? = null,
    inputPrefix: String = "§8» §f",
    completer: List<String> = mutableListOf()
): T = ConsoleQuestion(question, condition, default, inputPrefix, completer).ask(this)