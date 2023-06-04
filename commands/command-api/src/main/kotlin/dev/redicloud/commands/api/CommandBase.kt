package dev.redicloud.commands.api

import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

abstract class CommandBase {

    private var subCommands: List<CommandSubBase> = mutableListOf()
    private var name: String = ""
    private var description: String = ""
    private var aliases: Array<String> = arrayOf()
    private var permission: String? = null
    val suggester: ICommandSuggester = CommandSuggester(this)

    internal fun load() {
        name = this::class.findAnnotation<Command>()?.name
            ?: throw IllegalStateException("Command annotation not found on ${this::class.qualifiedName}")
        description = this::class.findAnnotation<CommandDescription>()?.description ?: ""
        subCommands = this::class.functions.filter { it.findAnnotation<CommandSubPath>() != null }.map {
            CommandSubBase(this, it)
        }
        aliases = this::class.findAnnotation<CommandAlias>()?.aliases ?: arrayOf()
        permission = this::class.findAnnotation<CommandPermission>()?.permission
    }

    fun isThis(input: String, predicate: Boolean): Boolean {
        val split = input.split(" ")
        if (split.isEmpty()) return predicate
        return if (predicate) {
            arrayOf(*aliases, name).any { it.lowercase().startsWith(split[0].lowercase()) }
        } else {
            arrayOf(*aliases, name).any { it.lowercase() == split[0].lowercase() }
        }
    }

    fun getName(): String = name

    fun getDescription(): String = description

    fun getAliases(): Array<String> = aliases

    fun getSubCommands(): List<CommandSubBase> = subCommands.toList()

    fun getPathsWithArguments(): List<String> = subCommands.flatMap { it.getSubPathsWithoutArguments() }

    fun getPaths(): List<String> = subCommands.flatMap { it.getSubPaths() }

    fun getPermission(): String? = permission

}