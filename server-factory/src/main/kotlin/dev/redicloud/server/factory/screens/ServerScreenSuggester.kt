package dev.redicloud.server.factory.screens

import dev.redicloud.commands.api.CommandContext
import dev.redicloud.commands.api.ICommandSuggester
import dev.redicloud.console.Console

class ServerScreenSuggester(
    private val console: Console
) : ICommandSuggester {

    override fun suggest(context: CommandContext): Array<String> {
        return console.getScreens().filterIsInstance<ServerScreen>().map { it.name }.toTypedArray()
    }

}