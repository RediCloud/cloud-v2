package dev.redicloud.commands.api

import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

abstract class CommandBase {

    private var subCommands: List<CommandSubBase> = mutableListOf()
    private var name: String = ""
    private var description: String = ""
    private var aliases: Array<String> = arrayOf()

    internal fun load() {
        name = this::class.findAnnotation<Command>()?.name
            ?: throw IllegalStateException("Command annotation not found on ${this::class.qualifiedName}")
        description = this::class.findAnnotation<CommandDescription>()?.description ?: ""
        subCommands = this::class.functions.filter { it.findAnnotation<CommandSubPath>() != null }.map {
            CommandSubBase(this, it)
        }
        aliases = this::class.findAnnotation<CommandAlias>()?.aliases ?: arrayOf()
    }

    fun getName(): String = name

    fun getDescription(): String = description

    fun getAliases(): Array<String> = aliases

    fun getSubCommands(): List<CommandSubBase> = subCommands.toList()

    fun getPathsWithArguments(): List<String> = subCommands.flatMap { it.getSubPathsWithoutArguments() }

    fun getPaths(): List<String> = subCommands.flatMap { it.getSubPaths() }

}