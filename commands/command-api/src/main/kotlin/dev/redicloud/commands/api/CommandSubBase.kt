package dev.redicloud.commands.api

import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod

class CommandSubBase(
    val command: CommandBase,
    val function: KFunction<*>
) {

    val path: String
    val description: String
    val arguments: List<CommandArgument>
    val aliasePaths: Array<String>
    val permission: String?
    val suggester: ICommandSuggester

    init {
        path = function.findAnnotation<CommandSubPath>()!!.path
        description = function.findAnnotation<CommandDescription>()?.description ?: ""
        suggester = CommandSubPathSuggester(this)
        var index = -1
        arguments = function.javaMethod!!.parameters.map {
            index++
            CommandArgument(this, it, index)
        }
        var optionalArguments = false
        arguments.forEach {
            if (!it.required) {
                optionalArguments = true
                return@forEach
            }
            if (optionalArguments && !it.actorArgument) throw IllegalStateException("Argument of ${command.getName()}.${path} is required after optional argument")
        }
        aliasePaths = function.findAnnotation<CommandAlias>()?.aliases ?: arrayOf()
        permission = function.findAnnotation<CommandPermission>()?.permission
    }

    fun execute(actor: ICommandActor<*>, arguments: List<String>): CommandResponse {
        val parsedArguments = mutableListOf<Any?>()
        val max = this.arguments.count { !it.actorArgument }
        val min = this.arguments.count { it.required && !it.actorArgument}
        if (arguments.size < min) return CommandResponse(CommandResponseType.INVALID_ARGUMENT_COUNT,
            "Not enough arguments (min: $min, max: $max)", usage = getUsage())
        if (arguments.size > max) return CommandResponse(CommandResponseType.INVALID_ARGUMENT_COUNT,
            "Too many arguments (min: $min, max: $max)", usage = getUsage())
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
                ?: return CommandResponse(CommandResponseType.INVALID_ARGUMENT_TYPE, "Invalid argument $argument or invalid type")
            parsedArguments.add(parsedArgument)
        }
        return try {
            function.javaMethod!!.invoke(command, *parsedArguments.toTypedArray())
            CommandResponse(CommandResponseType.SUCCESS, null)
        }catch (e: Exception) {
            CommandResponse(CommandResponseType.ERROR,
                "Error while executing command: ${command.getName()} ${getSubPathsWithoutArguments().joinToString(" ")}", e)
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
        val arguments = arguments.filter { !it.actorArgument }
        while (currentArgumentIndex < arguments.count() { !it.actorArgument } ) {
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