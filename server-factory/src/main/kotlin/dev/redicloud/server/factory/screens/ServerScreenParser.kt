package dev.redicloud.server.factory.screens

import dev.redicloud.commands.api.CommandArgumentParser
import dev.redicloud.console.Console

class ServerScreenParser(
    private val console: Console
) : CommandArgumentParser<ServerScreen> {

    override fun parse(parameter: String): ServerScreen? {
        val screen = console.getScreen(parameter) ?: return null
        if (screen !is ServerScreen) return null
        return screen
    }

}