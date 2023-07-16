package dev.redicloud.console.utils

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.console.Console

class ScreenParser(private val console: Console) : ICommandArgumentParser<Screen> {

    override fun parse(parameter: String): Screen? {
        return console.getScreens().firstOrNull { it.name.lowercase() == parameter.lowercase() }
    }

}