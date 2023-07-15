package dev.redicloud.commands.api

import java.lang.reflect.Parameter
import java.util.logging.LogManager
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
    val vararg: Boolean

    init {
        if (parameter.type.kotlin.superclasses.any { it == ICommandActor::class }) {
            name = "_actor"
            required = false
            clazz = parameter.type.kotlin
            parser = null
            annotatedSuggester = EmptySuggester()
            suggesterParameter = arrayOf()
            vararg = false
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
            vararg = parameter.isVarArgs
            clazz = if (vararg) parameter.type.componentType.kotlin else parameter.type.kotlin
            parser = CommandArgumentParser.PARSERS.filter {
                it.key.qualifiedName!!.replace("?", "") == clazz.qualifiedName!!.replace("?", "")
            }.values.firstOrNull() ?: throw IllegalStateException("No parser found for ${clazz.qualifiedName} in arguments of '${subCommand.command.getName()} ${subCommand.path}'")
        }
        suggester = CommandArgumentSuggester(this)
        if (vararg && !required) throw IllegalStateException("Vararg arguments can't be optional! (Argument: $name in '${subCommand.command.getName()} ${subCommand.path}')")
    }

    fun isActorArgument(): Boolean = name == "_actor"

    fun isThis(input: String, predict: Boolean): Boolean {
        if (!subCommand.isThis(input, predict)) {
            return false
        }
        if (isActorArgument()) {
            return false
        }
        if (input.isEmpty()) {
            return false
        }

        val optimalCurrentPaths = listOf(subCommand.path, *subCommand.aliasePaths)
            .flatMap { subCommandPaths ->
                listOf(subCommand.command.getName(), *subCommand.command.getAliases())
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
                    if (getPathFormat() == it || it.isEmpty() && predict) {
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
                val aIndex = optimalPath.split(" ").indexOf(getPathFormat())
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

    fun getPathFormat(): String {
        return if (vararg) {
            "<$name...>"
        }else if (required) "<$name>" else "[$name]"
    }

}