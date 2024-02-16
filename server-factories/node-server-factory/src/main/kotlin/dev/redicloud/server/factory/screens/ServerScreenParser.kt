package dev.redicloud.server.factory.screens

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.console.Console

class ServerScreenParser(
    private val console: Console
) : ICommandArgumentParser<ServerScreen> {

    override fun parse(parameter: String): ServerScreen? {
        val screen = console.getScreen(parameter) ?: return null
        if (screen !is ServerScreen) return null
        return screen
    }

}