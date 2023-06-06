package dev.redicloud.console.utils

import dev.redicloud.commands.api.CommandBase
import dev.redicloud.console.Console
import dev.redicloud.console.events.screen.ScreenChangedEvent

class Screen(
    val console: Console,
    val name: String,
    val allowedCommands: List<String> = mutableListOf("*"),
    val storeMessages: Boolean = true,
    val maxStoredMessages: Int = 50,
    val historySize: Int = 50
) {

    private val history = mutableListOf<String>()
    private val queuedMessage = mutableListOf<String>()

    fun display() {
        if (isActive()) return
        val oldScreen = console.getCurrentScreen()
        console.commandManager.enableCommands()
        console.commandManager.getCommands().forEach {
            if (!isCommandAllowed(it)) console.commandManager.disableCommand(it)
        }
        console.clearScreen()
        console.eventManager?.fireEvent(ScreenChangedEvent(console, this, oldScreen))
        history.forEach { console.writeLine(it) }
        if (storeMessages) {
            queuedMessage.forEach { console.writeLine(it) }
            queuedMessage.clear()
        }
    }

    fun isDefault(): Boolean = name == "main"

    fun isActive() = console.getCurrentScreen() == this

    fun writeLine(text: String) {
        if (!isActive()) {
            if (storeMessages) {
                queuedMessage.add(text)
                if (queuedMessage.size > maxStoredMessages) queuedMessage.removeFirst()
            }
            return
        }
        history.add(text)
        if (history.size > historySize) history.removeFirst()
        console.writeLine(text)
    }

    fun isCommandAllowed(command: CommandBase): Boolean = allowedCommands.contains("*")
            || allowedCommands.any { it.lowercase() == command.getName().lowercase()
                ||  command.getAliases().any { alias -> alias.lowercase() == it.lowercase() }}

}