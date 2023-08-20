package dev.redicloud.api.commands

interface IRegisteredCommand {

    val name: String
    val description: String
    val aliases: Array<String>
    val permission: String?
    val usage: String
    val paths: Array<String>
    val subCommands: List<ISubCommand>

}