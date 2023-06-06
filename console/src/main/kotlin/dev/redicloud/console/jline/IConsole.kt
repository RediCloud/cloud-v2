package dev.redicloud.console.jline

import dev.redicloud.console.Console
import dev.redicloud.console.animation.AbstractConsoleAnimation
import dev.redicloud.console.commands.ConsoleCommandManager
import dev.redicloud.console.utils.Screen


interface IConsole : AutoCloseable {


    var printingEnabled: Boolean
    var matchingHistorySearch: Boolean
    var prompt: String
    var lineFormat: String
    val commandManager: ConsoleCommandManager

    fun runningAnimations(): List<AbstractConsoleAnimation>

    fun startAnimation(animation: AbstractConsoleAnimation)

    fun animationRunning(): Boolean

    fun getCurrentScreen(): Screen

    fun switchScreen(screen: Screen)

    fun getScreens(): List<Screen>

    fun switchToDefaultScreen()

    fun createScreen(
        name: String,
        allowedCommands: List<String> = mutableListOf("*"),
        storeMessages: Boolean = true,
        maxStoredMessages: Int = 50,
        historySize: Int = 50
    ): Screen

    fun hasAnimationSupport(): Boolean {
        return hasColorSupport()
    }

    fun commandHistory(): List<String>

    fun commandHistory(history: List<String>?)

    fun commandInputValue(commandInputValue: String)

    fun enableCommands()

    fun disableCommands()

    fun writeRaw(rawText: String): Console

    fun forceWriteLine(text: String): Console

    fun writeLine(text: String): Console

    fun hasColorSupport(): Boolean

    fun resetPrompt()

    fun removePrompt()

    fun emptyPrompt()

    fun clearScreen()

}