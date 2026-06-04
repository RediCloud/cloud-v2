package dev.redicloud.api.console

import dev.redicloud.api.commands.ICommandManager

interface ICloudConsole {

    val commandManager: ICommandManager<*>

    fun writeLine(text: String): ICloudConsole

    fun forceWriteLine(text: String): ICloudConsole

    fun clearScreen()

    fun enableCommands()

    fun disableCommands()

    fun hasColorSupport(): Boolean
}
