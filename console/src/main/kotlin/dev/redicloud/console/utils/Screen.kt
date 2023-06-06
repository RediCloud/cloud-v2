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

    private val history = mutableListOf<LineBuilder>()
    private val queuedMessage = mutableListOf<LineBuilder>()

    fun display() {
        if (isActive()) return
        val oldScreen = console.currentScreen
        if (oldScreen.allowedCommands.isEmpty() && console.terminal.paused() && !allowedCommands.isEmpty()) {
            console.terminal.resume()
        }
        if (allowedCommands.isEmpty() && !console.terminal.paused()) {
            console.terminal.pause()
        }
        console.currentScreen = this
        console.clear()
        console.eventManager?.fireEvent(ScreenChangedEvent(console, this, oldScreen))
        history.forEach { print(it) }
        if (storeMessages) {
            queuedMessage.forEach { print(it) }
            queuedMessage.clear()
        }
    }

    fun isDefault(): Boolean = name == "main"

    fun isActive() = console.currentScreen == this

    fun print(line: LineBuilder) {
        if (!isActive()) {
            if (storeMessages) {
                queuedMessage.add(line)
                if (queuedMessage.size > maxStoredMessages) queuedMessage.removeFirst()
            }
            return
        }
        history.add(line)
        if (history.size > historySize) history.removeFirst()
        console.print(line)
    }

    fun print(lineBuilder: LineBuilder.() -> Unit) {
        print(LineBuilder.builder(console).apply(lineBuilder))
    }

    fun isCommandAllowed(command: CommandBase): Boolean = allowedCommands.contains("*")
            || allowedCommands.any { it.lowercase() == command.getName().lowercase()
                ||  command.getAliases().any { alias -> alias.lowercase() == it.lowercase() }}

}