package dev.redicloud.commands.api

import dev.redicloud.api.commands.*
import java.lang.reflect.Parameter
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

class CommandArgument(
    override val subCommand: CommandSubBase,
    parameter: Parameter,
    override val index: Int
) : ICommandArgument {

    val clazz: KClass<*>
    val parser: ICommandArgumentParser<*>?
    val suggester: CommandArgumentSuggester

    override val name: String
    override val required: Boolean
    override val annotatedSuggester: AbstractCommandSuggester
    override val annotatedSuggesterParameter: Array<String>
    override val vararg: Boolean
    override val pathFormat: String
        get() = if (vararg) { "<$name...>" } else if (required) "<$name>" else "[$name]"
    override val actorArgument: Boolean
        get() = name == "_actor"

    init {
        if (parameter.type.kotlin.superclasses.any {
                it == ICommandActor::class
            } || parameter.type.kotlin == ICommandActor::class
        ) {
            name = "_actor"
            required = false
            clazz = parameter.type.kotlin
            parser = null
            annotatedSuggester = EmptySuggester()
            annotatedSuggesterParameter = arrayOf()
            vararg = false
        } else {
            if (!parameter.isAnnotationPresent(CommandParameter::class.java)) {
                name = parameter.name
                required = !parameter.isImplicit // TODO check String? and Int? etc.
                annotatedSuggester = EmptySuggester()
                annotatedSuggesterParameter = emptyArray()
            } else {
                val annotation = parameter.getAnnotation(CommandParameter::class.java)
                name = annotation.name.ifEmpty { parameter.name }
                required = annotation.required // TODO check String? and Int? etc.
                annotatedSuggester = SUGGESTERS.firstOrNull { it::class == annotation.suggester } ?: EmptySuggester()
                annotatedSuggesterParameter = annotation.suggesterArguments
            }
            vararg = parameter.isVarArgs
            clazz = if (vararg) parameter.type.componentType.kotlin else parameter.type.kotlin
            parser = PARSERS.filter {
                it.key.qualifiedName!!.replace("?", "") == clazz.qualifiedName!!.replace("?", "")
            }.values.firstOrNull()
                ?: error("No parser found for ${clazz.qualifiedName} in arguments of '${subCommand.command.name} ${subCommand.path}'")
        }
        suggester = CommandArgumentSuggester(this)
        check(!vararg || required) {
            "Vararg arguments can't be optional! (Argument: $name in '${subCommand.command.name} ${subCommand.path}')"
        }
    }

    fun isThis(input: String, predict: Boolean): Boolean {
        if (!subCommand.isThis(input, predict) || actorArgument || input.isEmpty()) return false

        val optimalCurrentPaths = buildOptimalPaths()
        return optimalCurrentPaths.any { matchesOptimalPath(it, input, predict) }
    }

    private fun buildOptimalPaths(): Set<String> {
        return listOf(subCommand.path, *subCommand.aliasPaths)
            .flatMap { subCommandPaths ->
                listOf(subCommand.command.name, *subCommand.command.aliases)
                    .map { commandPath -> "$commandPath $subCommandPaths".removeLastSpaces() }
            }.toSet()
    }

    private data class PathMatchState(
        var index: Int = -1,
        var argumentIndex: Int = -1,
        var currentBuild: String = "",
        var lastWasThis: Boolean = false,
        var alreadyIndexed: Boolean = false
    )

    private fun matchesOptimalPath(optimalPath: String, input: String, predict: Boolean): Boolean {
        val inputParts = input.split(" ")
        val pathParts = optimalPath.split(" ")
        val state = buildMatchState(pathParts, inputParts, predict)

        if (!state.alreadyIndexed) {
            val aIndex = pathParts.indexOf(pathFormat)
            if (aIndex != -1 && input.removeLastSpaces().endsWith(" ") && predict && aIndex == state.index) {
                return true
            }
        }

        val predictMatch = predict &&
            state.currentBuild.endsWith(" ") &&
            optimalPath.lowercase().startsWith(state.currentBuild.lowercase()) &&
            state.currentBuild.split(" ").size == inputParts.size &&
            state.argumentIndex + 1 == this.index
        if (predictMatch) return true

        return state.currentBuild.lowercase() == input.lowercase() && state.lastWasThis
    }

    private fun buildMatchState(pathParts: List<String>, inputParts: List<String>, predict: Boolean): PathMatchState {
        val state = PathMatchState()
        pathParts.forEach { part ->
            state.index++
            if (inputParts.size < state.index + 1) return@forEach
            state.lastWasThis = false
            val inputCurrent = inputParts[state.index]
            if (part.isArgument()) {
                state.argumentIndex++
                if (pathFormat.lowercase() == part.lowercase() || part.isEmpty() && predict) {
                    state.lastWasThis = true
                    state.alreadyIndexed = true
                }
                if (state.currentBuild.isNotEmpty()) state.currentBuild += " "
                state.currentBuild += inputCurrent
                return@forEach
            }
            if (inputCurrent.lowercase() == part.lowercase()) {
                if (state.currentBuild.isNotEmpty()) state.currentBuild += " "
                state.currentBuild += inputCurrent
                return@forEach
            }
        }
        return state
    }

    fun parse(input: String): Any? = parser?.parse(input)
}
