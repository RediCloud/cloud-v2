package dev.redicloud.console.jline

import dev.redicloud.console.Console
import dev.redicloud.console.animation.AbstractConsoleAnimation
import dev.redicloud.console.commands.ConsoleCommandManager
import dev.redicloud.console.utils.Screen


interface IConsole : AutoCloseable {


    var printingEnabled: Boolean
    var matchingHistorySearch: Boolean
    var prompt: String
    val commandManager: ConsoleCommandManager

    fun runningAnimations(): List<AbstractConsoleAnimation>

    fun startAnimation(animation: AbstractConsoleAnimation)

    fun animationRunning(): Boolean

    fun getCurrentScreen(): Screen

    fun switchScreen(screen: Screen)

    fun switchToDefaultScreen()

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