package dev.redicloud.console.jline

import dev.redicloud.console.Console
import dev.redicloud.console.animation.AbstractConsoleAnimation
import dev.redicloud.console.commands.ConsoleCommandManager
import dev.redicloud.console.utils.Screen
import org.fusesource.jansi.Ansi


interface IConsole {


    var printingEnabled: Boolean
    var matchingHistorySearch: Boolean
    var prompt: String
    val commandManager: ConsoleCommandManager
    var lineFormat: String
    val saveLogToFile: Boolean
    val uninstallAnsiOnClose: Boolean

    fun runningAnimations(): List<AbstractConsoleAnimation>

    fun startAnimation(animation: AbstractConsoleAnimation)

    fun cancelAnimations()

    fun animationRunning(): Boolean

    fun getCurrentScreen(): Screen

    fun switchScreen(screen: Screen, cancelAnimations: Boolean = true)

    fun getScreens(): List<Screen>

    fun getScreen(name: String): Screen?

    fun switchToDefaultScreen(cancelAnimations: Boolean = true)

    fun createScreen(
        name: String,
        allowedCommands: List<String> = mutableListOf("*"),
        storeMessages: Boolean = true,
        maxStoredMessages: Int = 50,
        historySize: Int = 50
    ): Screen

    fun createScreen(screen: Screen): Screen

    fun deleteScreen(name: String)

    fun hasAnimationSupport(): Boolean {
        return hasColorSupport()
    }

    fun commandHistory(): List<String>

    fun commandHistory(history: List<String>?)

    fun commandInputValue(commandInputValue: String)

    fun enableCommands()

    fun disableCommands()

    fun writeRaw(
         rawText: String,
         ensureEndsWith: String = "",
         level: String = "§f INFO",
         lineFormat: Boolean = true,
         cursorUp: Boolean = false,
         eraseLine: Boolean = true,
         ansi: Ansi? = null,
         restoreCursor: Boolean = false,
         printDirectly: Boolean = false
    ): Console

    fun forceWriteLine(text: String, source: Screen? = null, history: Boolean = true): Console

    fun writeLine(text: String, source: Screen? = null, history: Boolean = true): Console

    fun hasColorSupport(): Boolean

    fun resetPrompt()

    fun removePrompt()

    fun emptyPrompt()

    fun clearScreen()

    fun close(processExit: Boolean = false)

}