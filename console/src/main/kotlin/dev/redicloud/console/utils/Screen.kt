package dev.redicloud.console.utils

import dev.redicloud.api.commands.IRegisteredCommand
import dev.redicloud.console.Console
import dev.redicloud.console.events.screen.ScreenChangedEvent
import dev.redicloud.utils.History

open class Screen(
    val console: Console,
    val name: String,
    val allowedCommands: List<String> = mutableListOf("*"),
    val storeMessages: Boolean = true,
    val maxStoredMessages: Int = 100,
    historySize: Int = 100
) {

    private val history = History<String>(historySize)
    protected val queuedMessage = mutableListOf<String>()

    fun clear() {
        history.clear()
        queuedMessage.clear()
    }

    fun addToHistory(line: String) {
        history.add(line)
    }

    fun display() {
        val oldScreen = console.getCurrentScreen()
        console.commandManager.enableCommands()
        console.commandManager.getCommands().forEach {
            if (!isCommandAllowed(it)) console.commandManager.disableCommand(it)
        }
        console.clearScreen()
        console.eventManager?.fireEvent(ScreenChangedEvent(console, this, oldScreen))
        history.forEach { console.writeRaw(it, printDirectly = true) }
        if (storeMessages) {
            queuedMessage.forEach {
                console.writeRaw(it, printDirectly = true)
                history.add(it)
            }
            queuedMessage.clear()
        }
    }

    fun isDefault(): Boolean = name == "main"

    fun isActive() = console.getCurrentScreen() == this

    open fun destroy() {
        console.deleteScreen(name)
    }

    open fun println(text: String) {
        if (!console.printingEnabled) return
        val content = this.console.formatText(console.textColor.ansiCode + text, System.lineSeparator())
        if (!isActive()) {
            addLine(content)
            return
        }
        history.add(content)
        console.writeRaw(content, printDirectly = true)
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

    fun isCommandAllowed(command: IRegisteredCommand): Boolean = allowedCommands.contains("*")
            || allowedCommands.any { it.lowercase() == command.name.lowercase()
                ||  command.aliases.any { alias -> alias.lowercase() == it.lowercase() }}

}