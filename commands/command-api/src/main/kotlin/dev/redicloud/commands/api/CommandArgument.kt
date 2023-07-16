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
        get() = if (vararg) { "<$name...>" }else if (required) "<$name>" else "[$name]"
    override val actorArgument: Boolean
        get() = clazz == ICommandActor::class

    init {
        if (parameter.type.kotlin.superclasses.any { it == ICommandActor::class }) {
            name = "_actor"
            required = false
            clazz = parameter.type.kotlin
            parser = null
            annotatedSuggester = EmptySuggester()
            annotatedSuggesterParameter = arrayOf()
            vararg = false
        }else {
            if (!parameter.isAnnotationPresent(CommandParameter::class.java)) {
                name = parameter.name
                required = !parameter.isImplicit //TODO check String? and Int? etc.
                annotatedSuggester = EmptySuggester()
                annotatedSuggesterParameter = emptyArray()
            } else {
                val annotation = parameter.getAnnotation(CommandParameter::class.java)
                name = annotation.name.ifEmpty { parameter.name }
                required = annotation.required //TODO check String? and Int? etc.
                annotatedSuggester = SUGGESTERS.firstOrNull { it::class == annotation.suggester } ?: EmptySuggester()
                annotatedSuggesterParameter = annotation.suggesterArguments
            }
            vararg = parameter.isVarArgs
            clazz = if (vararg) parameter.type.componentType.kotlin else parameter.type.kotlin
            parser = PARSERS.filter {
                it.key.qualifiedName!!.replace("?", "") == clazz.qualifiedName!!.replace("?", "")
            }.values.firstOrNull() ?: throw IllegalStateException("No parser found for ${clazz.qualifiedName} in arguments of '${subCommand.command.name} ${subCommand.path}'")
        }
        suggester = CommandArgumentSuggester(this)
        if (vararg && !required) throw IllegalStateException("Vararg arguments can't be optional! (Argument: $name in '${subCommand.command.name} ${subCommand.path}')")
    }

    fun isThis(input: String, predict: Boolean): Boolean {
        if (!subCommand.isThis(input, predict)) {
            return false
        }
        if (actorArgument) {
            return false
        }
        if (input.isEmpty()) {
            return false
        }

        val optimalCurrentPaths = listOf(subCommand.path, *subCommand.aliasPaths)
            .flatMap { subCommandPaths ->
                listOf(subCommand.command.name, *subCommand.command.aliases)
                .map { commandPath -> "$commandPath $subCommandPaths".removeLastSpaces() }
            }.toSet()

        optimalCurrentPaths.forEach optimalPathForEach@ { optimalPath ->
            var index = -1
            var argumentIndex = -1
            var currentBuild = ""
            var lastWasThis = false
            var alreadyIndexed = false
            optimalPath.split(" ").forEach optimalParameterForEach@ {
                index++
                if (input.split(" ").size < index + 1) {
                    return@optimalParameterForEach
                }
                lastWasThis = false
                val inputCurrent = input.split(" ")[index]
                if (it.isArgument()) {
                    argumentIndex++
                    if (pathFormat.lowercase() == it.lowercase() || it.isEmpty() && predict) {
                        lastWasThis = true
                        alreadyIndexed = true
                    }
                    if (currentBuild.isNotEmpty()) currentBuild += " "
                    currentBuild += inputCurrent
                    return@optimalParameterForEach
                }
                if (inputCurrent.lowercase() == it.lowercase()) {
                    if (currentBuild.isNotEmpty()) currentBuild += " "
                    currentBuild += inputCurrent
                    return@optimalParameterForEach
                }
            }
            if (!alreadyIndexed) {
                val aIndex = optimalPath.split(" ").indexOf(pathFormat)
                if (aIndex != -1) {
                    if (input.removeLastSpaces().endsWith(" ") && predict && aIndex == index) {
                        return true
                    }
                }
            }
            if (predict
                && currentBuild.endsWith(" ")
                && optimalPath.lowercase().startsWith(currentBuild.lowercase())
                && currentBuild.split(" ").size == input.split(" ").size
                && argumentIndex+1 == this.index) {
                return true
            }
            if (currentBuild.lowercase() == input.lowercase() && lastWasThis) {
                return true
            }
        }

        return false
    }

    fun parse(input: String): Any? = parser?.parse(input)

}