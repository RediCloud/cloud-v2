package dev.redicloud.console.utils

import dev.redicloud.commands.api.CommandArgumentParser
import dev.redicloud.console.Console

class ScreenParser(private val console: Console) : CommandArgumentParser<Screen> {

    override fun parse(parameter: String): Screen? {
        return console.getScreens().firstOrNull { it.name.lowercase() == parameter.lowercase() }
    }

}