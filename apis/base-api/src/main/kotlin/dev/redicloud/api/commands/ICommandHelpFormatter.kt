package dev.redicloud.api.commands

interface ICommandHelpFormatter {
    fun formatHelp(actor: ICommandActor<*>, context: CommandContext): CommandResponse
}