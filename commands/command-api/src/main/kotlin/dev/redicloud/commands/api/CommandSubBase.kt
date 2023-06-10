package dev.redicloud.commands.api

import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod

class CommandSubBase(
    val command: CommandBase,
    val function: KFunction<*>
) {

    val suspend: Boolean
    val path: String
    val description: String
    val arguments: List<CommandArgument>
    val aliasePaths: Array<String>
    val permission: String?
    val suggester: ICommandSuggester

    init {
        suspend = function.isSuspend
        if (suspend) throw UnsupportedOperationException("Suspend functions are not supported yet!")
        path = function.findAnnotation<CommandSubPath>()!!.path
        description = function.findAnnotation<CommandDescription>()?.description ?: ""
        suggester = CommandSubPathSuggester(this)
        var index = -1
        arguments = function.javaMethod!!.parameters.mapNotNull {
            if (it.type.name.contains("kotlin.coroutines")) return@mapNotNull null
            index++
            CommandArgument(this, it, index)
        }
        var optionalArguments = false
        arguments.forEach {
            if (!it.required && !it.isActorArgument()) {
                optionalArguments = true
                return@forEach
            }
            if (optionalArguments && !it.isActorArgument()) throw IllegalStateException("Argument of ${command.getName()}.${path} is required after optional argument")
        }
        aliasePaths = function.findAnnotation<CommandAlias>()?.aliases ?: arrayOf()
        permission = function.findAnnotation<CommandPermission>()?.permission
    }

    fun execute(actor: ICommandActor<*>, arguments: List<String>): CommandResponse {
        val parsedArguments = mutableListOf<Any?>()
        val max = this.arguments.count { !it.isActorArgument() }
        val min = this.arguments.count { it.required && !it.isActorArgument() }
        if (arguments.size < min) return CommandResponse(CommandResponseType.INVALID_ARGUMENT_COUNT,
            "Not enough arguments (min: $min, max: $max)", usage = getUsage())
        if (arguments.size > max) return CommandResponse(CommandResponseType.INVALID_ARGUMENT_COUNT,
            "Too many arguments (min: $min, max: $max)", usage = getUsage())
        var index = -1
        this.arguments.forEach {
            if (it.isActorArgument()) {
                parsedArguments.add(actor)
                return@forEach
            }
            index++
            if (index >= arguments.count()) {
                if (it.required) return CommandResponse(CommandResponseType.INVALID_ARGUMENT_COUNT, "Not enough arguments (min: $min, max: $max)")
                parsedArguments.add(null)
                return@forEach
            }
            val argument = arguments[index]
            val parsedArgument = it.parse(argument)
                ?: return CommandResponse(CommandResponseType.INVALID_ARGUMENT_TYPE, "Invalid argument '$argument' or invalid type")
            parsedArguments.add(parsedArgument)
        }
        return try {
            if (suspend) {
                runBlocking { function.callSuspend(*parsedArguments.toTypedArray()) } //TODO fix this
            }else {
                function.javaMethod!!.invoke(command, *parsedArguments.toTypedArray())
            }
            CommandResponse(CommandResponseType.SUCCESS, null)
        }catch (e: Exception) {
            CommandResponse(CommandResponseType.ERROR,
                "Error while executing command: ${command.getName()} ${getSubPathsWithoutArguments().joinToString(" ")}", e)
        }
    }

    fun parseToOptimalPath(input: String): String? {
        if (!command.isThis(input, false)) return null
        val split = input.split(" ")
        if (split.size < 2) return input
        val parameters = split.drop(1)
        val arguments = arguments.filter { !it.isActorArgument() }
        val possibleFullPaths = command.getPaths()
        val matched = possibleFullPaths.toMutableList()
        var index = -1
        parameters.forEach {
            index++
            val possible = matched.filter { path ->
                val parameterSplit = path.split(" ")
                if (parameterSplit.size <= index) return@filter false
                val parameter = parameterSplit[index].lowercase()
                if (parameter.isOptionalArgument() || parameter.isRequiredArgument()) return@filter true
                parameter.lowercase().startsWith(it.lowercase())
            }
            matched.clear()
            matched.addAll(possible)
        }
        return matched.firstOrNull()
    }

    fun isThis(input: String, predicate: Boolean): Boolean {
        if (!command.isThis(input, false)) return false
        val split = if (predicate) input.split(" ") else input.removeLastSpaces().split(" ")
        if (split.size == 1 && path.isEmpty()) return true
        if (split.size < 2) return input.endsWith(" ")
        val parameters = split.drop(1)
        val possibleFullPaths = command.getPaths()
        val matched = possibleFullPaths.toMutableList()
        var index = -1
        parameters.forEach {
            index++
            val possible = matched.filter { path ->
                val parameterSplit = path.split(" ")
                if (parameterSplit.size <= index) return@filter false
                val parameter = parameterSplit[index].lowercase()
                if (parameter.isOptionalArgument() || parameter.isRequiredArgument()) return@filter true
                if (predicate) parameter.lowercase().startsWith(it.lowercase()) else parameter.lowercase() == it.lowercase()
            }
            matched.clear()
            matched.addAll(possible)
        }

        if (matched.size > 2 && !predicate) return false

        if (matched.isEmpty()) return false

        return if (predicate) {
            command.getSubCommands()
                .any { subCommand -> subCommand.getSubPaths()
                    .any { path -> matched.any { it.lowercase().startsWith(path.lowercase()) } } }
        }else {
            command.getSubCommands()
                .any { subCommand -> subCommand.getSubPaths()
                    .any { path -> matched.first().lowercase() == path.lowercase() } }
        }
    }

    fun getUsage(): String = "${command.getName()} ${getSubPaths().first()}"

    fun getSubPathsWithoutArguments(): List<String>
        = listOf(*aliasePaths, path)

    fun getSubPaths(): List<String> {
        val prefixes = listOf(*aliasePaths, path)
        val paths = mutableListOf<String>()
        var currentPath = "%prefix%"
        if (arguments.isEmpty()) return prefixes.map { currentPath.replace("%prefix%", it) }
        var currentArgumentIndex = 0
        val arguments = arguments.filter { !it.isActorArgument() }
        while (currentArgumentIndex < arguments.count() { !it.isActorArgument() } ) {
            val currentArgument = arguments[currentArgumentIndex]
            if (currentArgument.required) {
                currentPath += " ${currentArgument.getPathFormat()}"
                currentArgumentIndex++
                continue
            }
            paths.add(currentPath)
            currentPath += " [${currentArgument.name}]"
            currentArgumentIndex++
        }
        if (!paths.contains(currentPath)) paths.add(currentPath)
        return prefixes.flatMap { prefix -> paths.map { it.replace("%prefix%", prefix) } }
    }

}