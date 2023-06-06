package dev.redicloud.console

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import dev.redicloud.commands.api.CommandResponseType
import dev.redicloud.console.commands.ConsoleCommandManager
import dev.redicloud.console.events.ConsoleExitEvent
import dev.redicloud.console.events.ConsoleRunEvent
import dev.redicloud.console.events.screen.ScreenCreatedEvent
import dev.redicloud.console.utils.*
import dev.redicloud.event.EventManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.fusesource.jansi.AnsiConsole
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.slf4j.LoggerFactory

abstract class Console(
    val name: String = "console",
    val configuration: ConsoleConfiguration = ConsoleConfiguration(),
    val eventManager: EventManager?
) {

    val terminal: Terminal
    var logLevel: Level = Level.INFO
    val commandManager = ConsoleCommandManager(this)

    internal val lineReader: LineReader
    private var running = false
    internal var currentScreen: Screen = Screen(this, "main")
    private val screens = mutableListOf<Screen>()
    private val loggerListener = LoggerListener(this)

    @OptIn(DelicateCoroutinesApi::class)
    private val scope = CoroutineScope(newSingleThreadContext("$name-console-scope"))

    init {
        terminal = TerminalBuilder.builder()
            .name(name)
            .system(true) //TODO
            .encoding(Charsets.UTF_8)
            .build()
        lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .highlighter(ConsoleHighlighter(this.configuration))
            .completer(ConsoleCompleter(this.commandManager))
            .build()

        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).addAppender(loggerListener)

        screens.add(currentScreen)

        AnsiConsole.systemInstall()
    }

    fun isRunning() = running

    protected fun run() {
        scope.launch {
            eventManager?.fireEvent(ConsoleRunEvent(this@Console))
            while (true) {
                running = true
                try {
                    val line = lineReader.readLine("${configuration.inputPrefix} ")
                    if (currentScreen.allowedCommands.isEmpty()) {
                        print { text("You are not allowed to use any commands in this screen") }
                        continue
                    }
                    val command = commandManager.getCommand(line.lowercase())
                    if (command != null && !currentScreen.isCommandAllowed(command)) {
                        print { text("You are not allowed to use this command in this screen") }
                        continue
                    }
                    val response = commandManager.handleInput(commandManager.actor, line)
                    when (response.type) {
                        CommandResponseType.ERROR -> print {
                            prefix("ERROR")
                            text(response.message!!)
                            throwable(response.throwable!!)
                        }
                        CommandResponseType.INVALID_ARGUMENT_COUNT -> print {
                            prefix("COMMAND")
                            text("Invalid argument count! Usage: '${response.usage}'")
                        }
                        CommandResponseType.PERMISSION -> print {
                            prefix("COMMAND")
                            text("You don't have the permission to use this command!")
                        }
                        CommandResponseType.INVALID_COMMAND -> print {
                            prefix("COMMAND")
                            text("Invalid command! Use 'help' to get a list of all commands")
                        }
                        CommandResponseType.INVALID_ARGUMENT_TYPE -> print {
                            prefix("COMMAND")
                            text("Invalid argument type! Usage: '${response.usage}'")
                        }
                        CommandResponseType.INVALID_SUB_PATH -> print {
                            prefix("COMMAND")
                            text("Invalid sub path! Use '${command!!.getName()} help' to get a list of all sub paths")
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    running = false
                    eventManager?.fireEvent(ConsoleExitEvent(this@Console))
                    onExit(e)
                }
            }
        }
    }

    fun getScreens(): List<Screen> = screens.toList()

    fun getCurrentScreen(): Screen = currentScreen

    fun createScreen(
        name: String,
        allowedCommands: List<String> = mutableListOf("*"),
        storeMessages: Boolean = true,
        maxStoredMessages: Int = 50
    ): Screen {
        val screen = Screen(this, name, allowedCommands, storeMessages, maxStoredMessages)
        screens.add(screen)
        eventManager?.fireEvent(ScreenCreatedEvent(screen, this))
        return screen
    }

    abstract fun onExit(exception: Exception?)

    fun clear() {
        terminal.writer().print("\u001b[H\u001b[2J")
        terminal.flush()
    }

    fun print(lineBuilder: LineBuilder) {
        terminal.writer().print(lineBuilder.getContent())
        terminal.flush()
    }

    fun print(lineBuilder: LineBuilder.() -> Unit) {
        print(LineBuilder.builder(this).apply { lineBuilder() })
    }

}