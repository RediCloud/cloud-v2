package dev.redicloud.commands.api

import dev.redicloud.api.commands.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

class CommandBase(
    val commandImpl: ICommand,
    val commandManager: CommandManager<*>
) : IRegisteredCommand {

    override val subCommands: List<CommandSubBase>
    override val aliases: Array<String>
    override val description: String
    override val name: String
    override val permission: String?
    override val usage: String
    override val paths: Array<String>
    val suggester: AbstractCommandSuggester = CommandSuggester(this)

    init {
        name = commandImpl::class.findAnnotation<Command>()?.name
            ?: throw IllegalStateException("Command annotation not found on ${this::class.qualifiedName}")
        description = commandImpl::class.findAnnotation<CommandDescription>()?.description ?: ""
        subCommands = commandImpl::class.functions.filter { it.findAnnotation<CommandSubPath>() != null }.map {
            CommandSubBase(this, it)
        }
        aliases = commandImpl::class.findAnnotation<CommandAlias>()?.aliases ?: arrayOf()
        permission = commandImpl::class.findAnnotation<CommandPermission>()?.permission
        usage = "TODO"
        paths = subCommands.flatMap { it.getSubPaths() }.toTypedArray()
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

    fun getSubCommand(subPath: String): CommandSubBase? = subCommands.firstOrNull {
        it.isThis("$name $subPath", false)
    }

}