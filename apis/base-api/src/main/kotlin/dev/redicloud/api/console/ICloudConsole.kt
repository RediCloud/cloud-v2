package dev.redicloud.api.console

import dev.redicloud.api.commands.ICommandManager

/**
 * Simplified console interface for cloud modules.
 *
 * Provides basic console output and command management. Obtain an instance
 * via Guice injection. The concrete implementation uses JLine3 for
 * terminal I/O and is only active on node services.
 */
interface ICloudConsole {

    /** The command manager for registering and handling console commands. */
    val commandManager: ICommandManager<*>

    /**
     * Writes a line to the console if printing is enabled.
     *
     * @param text the text to write (supports color codes with `§`)
     * @return this console instance
     */
    fun writeLine(text: String): ICloudConsole

    /**
     * Writes a line to the console regardless of the printing state.
     *
     * @param text the text to write (supports color codes with `§`)
     * @return this console instance
     */
    fun forceWriteLine(text: String): ICloudConsole

    /** Clears the console screen. */
    fun clearScreen()

    /** Enables command input processing. */
    fun enableCommands()

    /** Disables command input processing. */
    fun disableCommands()

    /** Returns whether the console supports ANSI color codes. */
    fun hasColorSupport(): Boolean
}
