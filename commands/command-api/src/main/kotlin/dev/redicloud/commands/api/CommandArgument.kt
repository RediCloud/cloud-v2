package dev.redicloud.commands.api

import java.lang.reflect.Parameter
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

class CommandArgument(val subCommand: CommandSubBase, parameter: Parameter, val index: Int) {

    val name: String
    val required: Boolean //TODO
    val clazz: KClass<*>
    val parser: CommandArgumentParser<*>?
    val annotatedSuggester: AbstractCommandSuggester
    val suggester: CommandArgumentSuggester
    val suggesterParameter: Array<String>

    init {
        if (parameter.type.kotlin.superclasses.any { it == ICommandActor::class }) {
            name = "_actor"
            required = false
            clazz = parameter.type.kotlin
            parser = null
            annotatedSuggester = EmptySuggester()
            suggesterParameter = arrayOf()
        }else {
            if (!parameter.isAnnotationPresent(CommandParameter::class.java)) {
                name = parameter.name
                required = !parameter.isImplicit //TODO check String? and Int? etc.
                annotatedSuggester = EmptySuggester()
                suggesterParameter = emptyArray()
            } else {
                val annotation = parameter.getAnnotation(CommandParameter::class.java)
                name = annotation.name.ifEmpty { parameter.name }
                required = annotation.required //TODO check String? and Int? etc.
                annotatedSuggester = AbstractCommandSuggester.SUGGESTERS.firstOrNull { it::class == annotation.suggester } ?: EmptySuggester()
                suggesterParameter = annotation.suggesterArguments
            }
            clazz = parameter.type.kotlin
            parser = CommandArgumentParser.PARSERS.filter {
                it.key.qualifiedName!!.replace("?", "") == clazz.qualifiedName!!.replace("?", "")
            }.values.firstOrNull() ?: throw IllegalStateException("No parser found for ${clazz.qualifiedName} in arguments of '${subCommand.command.getName()} ${subCommand.path}'")
        }
        suggester = CommandArgumentSuggester(this)
    }

    fun isActorArgument(): Boolean = name == "_actor"

    fun isThis(input: String, predict: Boolean): Boolean {
        if (!subCommand.isThis(input, predict)) return false
        if (isActorArgument()) return false
        if (input.isEmpty()) return false

        val optimalCurrentPaths = listOf(subCommand.path, *subCommand.aliasePaths)
            .flatMap { subCommandPaths ->
                listOf(subCommand.command.getName(), *subCommand.command.getAliases())
                .map { commandPath -> "$commandPath $subCommandPaths".removeLastSpaces() }
            }.toSet()

        optimalCurrentPaths.forEach optimalPathForEach@ { optimalPath ->
            if (optimalPath.startsWith(input) && predict) return true
            var index = -1
            var currentBuild = ""
            var lastWasThis = false
            var alreadyIndexed = false
            optimalPath.split(" ").forEach optimalParameterForEach@ {
                lastWasThis = false
                index++
                if (input.split(" ").size < index + 1) return@optimalParameterForEach
                val inputCurrent = input.split(" ")[index]
                if (it.isArgument()) {
                    if (getPathFormat() == it) {
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
                val argumentIndex = optimalPath.split(" ").indexOf(getPathFormat()) // 4
                if (argumentIndex != -1) {
                    if (input.endsWith(" ") && predict && argumentIndex == index) return true
                }
            }
            if (currentBuild.lowercase().removeLastSpaces() == input.lowercase().removeLastSpaces() && lastWasThis) return true
        }

        return false
    }

    fun parse(input: String): Any? = parser?.parse(input)

    fun getPathFormat(): String = if (required) "<$name>" else "[$name]"

}