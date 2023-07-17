package dev.redicloud.api.commands

interface ISubCommand {

    val command: IRegisteredCommand
    val path: String
    val aliasPaths: Array<String>
    val description: String
    val permission: String?
    val arguments: List<ICommandArgument>

}