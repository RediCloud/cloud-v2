package dev.redicloud.server.factory.screens

import dev.redicloud.api.commands.CommandContext
import dev.redicloud.api.commands.AbstractCommandSuggester
import dev.redicloud.console.Console

class ServerScreenSuggester(
    private val console: Console
) : AbstractCommandSuggester() {

    override fun suggest(context: CommandContext): Array<String> {
        return console.getScreens().filterIsInstance<ServerScreen>().map { it.name }.toTypedArray()
    }

}