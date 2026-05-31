package dev.redicloud.console.utils

import dev.redicloud.api.commands.ICommandArgumentParser
import dev.redicloud.console.Console

class ScreenParser(private val console: Console) : ICommandArgumentParser<Screen> {

    override suspend fun parse(parameter: String): Screen? {
        return console.getScreens().firstOrNull { it.name.equals(parameter, ignoreCase = true) }
    }
}
