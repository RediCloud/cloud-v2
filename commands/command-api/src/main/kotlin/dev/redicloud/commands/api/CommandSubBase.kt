package dev.redicloud.commands.api

import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod

class CommandSubBase(
    val command: CommandBase,
    function: KFunction<*>
) {

    val path: String
    val description: String
    val arguments: List<CommandArgument>
    val aliasePaths: Array<String>

    init {
        path = function.findAnnotation<CommandSubPath>()!!.path
        description = function.findAnnotation<CommandDescription>()?.description ?: ""
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
            if (optionalArguments) throw IllegalStateException("Argument of ${command.getName()}.${path} is required after optional argument")
        }
        aliasePaths = function.findAnnotation<CommandAlias>()?.aliases ?: arrayOf()
    }

    fun getSubPathsWithoutArguments(): List<String>
        = listOf(*aliasePaths, path)

    fun getSubPaths(): List<String> {
        val prefixes = listOf(*aliasePaths, path)
        val paths = mutableListOf<String>()
        val count = arguments.count { !it.required } + 1
        var currentPath = "%prefix%"
        if (arguments.isEmpty()) return prefixes.map { currentPath.replace("%prefix%", it) }
        var currentArgumentIndex = 0
        while (currentArgumentIndex < arguments.count()) {
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