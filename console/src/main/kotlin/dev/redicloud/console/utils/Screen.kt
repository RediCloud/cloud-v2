package dev.redicloud.console.utils

import dev.redicloud.commands.api.CommandBase
import dev.redicloud.console.Console
import dev.redicloud.console.events.screen.ScreenChangedEvent
import dev.redicloud.utils.History

open class Screen(
    val console: Console,
    val name: String,
    val allowedCommands: List<String> = mutableListOf("*"),
    val storeMessages: Boolean = true,
    val maxStoredMessages: Int = 50,
    historySize: Int = 100
) {

    val history = History<String>(historySize)
    protected val queuedMessage = mutableListOf<String>()

    fun display() {
        val oldScreen = console.getCurrentScreen()
        console.commandManager.enableCommands()
        console.commandManager.getCommands().forEach {
            if (!isCommandAllowed(it)) console.commandManager.disableCommand(it)
        }
        console.clearScreen()
        console.eventManager?.fireEvent(ScreenChangedEvent(console, this, oldScreen))
        history.forEach { console.writeLine(it, this, false) }
        if (storeMessages) {
            queuedMessage.forEach { console.writeLine(it, this) }
            queuedMessage.clear()
        }
    }

    fun isDefault(): Boolean = name == "main"

    fun isActive() = console.getCurrentScreen() == this

    open fun destroy() {
        console.deleteScreen(name)
    }

    open fun println(text: String) {
        console.writeLine(text, this)
    }

    open fun addLine(text: String) {
        if (!isActive()) {
            if (storeMessages) {
                queuedMessage.add(text)
                if (queuedMessage.size > maxStoredMessages) queuedMessage.removeFirst()
            }
            return
        }
        history.add(text)
    }

    fun isCommandAllowed(command: CommandBase): Boolean = allowedCommands.contains("*")
            || allowedCommands.any { it.lowercase() == command.getName().lowercase()
                ||  command.getAliases().any { alias -> alias.lowercase() == it.lowercase() }}

}