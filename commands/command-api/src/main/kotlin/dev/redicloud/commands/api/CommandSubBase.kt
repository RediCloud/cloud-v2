package dev.redicloud.commands.api

import dev.redicloud.api.commands.*
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod

class CommandSubBase(
    override val command: CommandBase,
    val function: KFunction<*>
) : ISubCommand {

    val suspend: Boolean
    val suggester: AbstractCommandSuggester

    override val path: String
    override val description: String
    override val arguments: List<CommandArgument>
    override val aliasPaths: Array<String>
    override  val permission: String?

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
        var vararg = false
        arguments.forEach {
            if (!it.required && !it.vararg && !it.actorArgument) {
                optionalArguments = true
                return@forEach
            }
            if (it.vararg && !it.actorArgument) {
                vararg = true
                return@forEach
            }
            if (it.vararg && !it.actorArgument) throw IllegalStateException("Vararg argument of '${command.name} ${path}' can be only the last argument")
            if (optionalArguments && !it.actorArgument) throw IllegalStateException("Argument of '${command.name} ${path}' is required after optional argument")
        }
        aliasPaths = function.findAnnotation<CommandAlias>()?.aliases ?: arrayOf()
        permission = function.findAnnotation<CommandPermission>()?.permission
    }

    fun execute(actor: ICommandActor<*>, arguments: List<String>): CommandResponse {
        val parsedArguments = mutableListOf<Any?>()
        val max = this.arguments.count { !it.actorArgument }
        val min = this.arguments.count { it.required && !it.actorArgument }
        val lastArgument = this.arguments.lastOrNull { !it.actorArgument }
        if (arguments.size < min) {
            return CommandResponse(CommandResponseType.INVALID_ARGUMENT_COUNT,
                "Not enough arguments (min: $min, max: $max)", usage = getUsage())
        }
        if (arguments.size > max
            && lastArgument?.vararg == false) {
            return CommandResponse(CommandResponseType.INVALID_ARGUMENT_COUNT,
                "Too many arguments (min: $min, max: $max)", usage = getUsage())
        }
        var index = -1
        this.arguments.forEach {
            if (it.actorArgument) {
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
        if (lastArgument != null && lastArgument.vararg) {
            val varargArguments = arguments.drop(lastArgument.index)
            val parsedVarargArguments = mutableListOf(parsedArguments.last())
            parsedArguments.removeIf { parsedVarargArguments.contains(it) }
            varargArguments.forEach {
                val parsedArgument = lastArgument.parse(it)
                    ?: return CommandResponse(CommandResponseType.INVALID_ARGUMENT_TYPE, "Invalid argument '$it' or invalid type")
                parsedVarargArguments.add(parsedArgument)
            }
            val array = java.lang.reflect.Array.newInstance(lastArgument.clazz.java, parsedVarargArguments.size)
            parsedVarargArguments.forEachIndexed { i, any ->
                java.lang.reflect.Array.set(array, i, any)
            }
            parsedArguments.add(array)
        }
        val final = parsedArguments.toTypedArray()
        return try {
            if (suspend) {
                runBlocking { function.callSuspend(command.commandImpl, *final) } //TODO fix this
            }else {
                // function.call(command.commandImpl, *final) //TODO fix this
                function.javaMethod!!.invoke(command.commandImpl, *final)
            }
            CommandResponse(CommandResponseType.SUCCESS, null)
        }catch (e: Exception) {
            CommandResponse(CommandResponseType.ERROR,
                "Error while executing command: ${command.name} $path", e)
        }
    }

    fun parseToOptimalPath(input: String): String? {
        if (!command.isThis(input, false)) return null
        val split = input.removeLastSpaces().split(" ")
        if (split.size < 2) return input
        val parameters = split.drop(1)
        val arguments = arguments.filter { !it.actorArgument }
        val possibleFullPaths = command.paths
        val matched = possibleFullPaths.toMutableList()
        var index = -1
        parameters.forEach {
            index++
            val possible = matched.filter { path ->
                val parameterSplit = path.split(" ")
                if (parameterSplit.size <= index) {
                    return@filter arguments.isNotEmpty() && arguments.last().vararg
                }
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
        if (!command.isThis(input, predicate)) return false
        val split = if (predicate) input.split(" ") else input.removeLastSpaces().split(" ")
        if (split.size == 1 && path.isEmpty()) return true
        if (split.size < 2 && predicate) return input.endsWith(" ")
        val parameters = split.drop(1)
        if (parameters.isEmpty()) return predicate
        val possibleFullPaths = getSubPaths()
        val matched = possibleFullPaths.toMutableList()
        var index = -1
        val counts = mutableListOf<Int>()
        val vararg = arguments.firstOrNull { it.vararg }
        mutableListOf(path, *aliasPaths).forEach {
            val maxLength = it.split(" ").size
            val minLength = it.split(" ").count { !it.isOptionalArgument() }
            for (i in minLength..maxLength) {
                counts.add(i)
            }
            if (vararg != null && parameters.size >= minLength) counts.add(parameters.size)
        }
        if (counts.none { it == parameters.size } && !predicate) return false
        parameters.forEach {
            index++
            val possible = matched.filter { path ->
                val parameterSplit = path.split(" ")
                if (parameterSplit.size <= index) return@filter arguments.isNotEmpty() && arguments.last().vararg
                val parameter = parameterSplit[index].lowercase()
                if (parameter.isOptionalArgument() || parameter.isRequiredArgument()) return@filter true
                if (predicate) parameter.lowercase().startsWith(it.lowercase()) else parameter.lowercase() == it.lowercase()
            }
            matched.clear()
            matched.addAll(possible)
        }

        return matched.isNotEmpty()
    }

    fun getUsage(): String = "${command.name} ${getSubPaths().first()}"

    fun getSubPaths(): List<String> {
        return listOf(*aliasPaths, path).map { it.removeLastSpaces() }
    }

}