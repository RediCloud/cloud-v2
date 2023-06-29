package dev.redicloud.commands.api

interface ICommandHelpFormatter {
    fun formatHelp(actor: ICommandActor<*>, context: CommandContext): CommandResponse
}