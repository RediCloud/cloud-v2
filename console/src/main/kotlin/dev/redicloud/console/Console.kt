package dev.redicloud.console

import dev.redicloud.console.commands.ConsoleCommandManager
import dev.redicloud.console.events.ConsoleRunEvent
import dev.redicloud.console.utils.AnsiInstaller
import dev.redicloud.console.utils.ConsoleColor
import dev.redicloud.console.jline.ConsoleLineReader
import dev.redicloud.console.jline.IConsole
import dev.redicloud.console.animation.AbstractConsoleAnimation
import dev.redicloud.console.jline.ConsoleCompleter
import dev.redicloud.console.jline.ConsoleHighlighter
import dev.redicloud.console.utils.Screen
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.logging.Logger
import dev.redicloud.utils.CLOUD_VERSION
import dev.redicloud.utils.USER_NAME
import kotlinx.coroutines.*
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import org.jline.utils.StyleResolver
import java.io.IOException
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.system.exitProcess


class Console(val host: String, val eventManager: EventManager?) : IConsole {

    companion object {
        private val LOGGER: Logger = LogManager.logger(Console::class.java)
    }

    private val ansiSupported: Boolean
    private val printLock: Lock = ReentrantLock(true)
    private val defaultScreen: Screen = Screen(this, "main")
    private var currentScreen: Screen = defaultScreen
    override val commandManager = ConsoleCommandManager(this)
    private val runningAnimations: MutableMap<UUID, AbstractConsoleAnimation> = mutableMapOf()
    internal val terminal: Terminal
    internal val lineReader: ConsoleLineReader
    override var prompt: String = System.getProperty("redicloud.console.promt", "%hc%%user%§8@§f%host% §8➔ §r")
    private val lineFormat: String = System.getProperty("redicloud.console.lineformat", "§8[§f%time%§8] §f%prefix%§8: §r%message%")
    private val highlightColor: String = System.getProperty("redicloud.console.highlight", "§3")
    private val textColor: String = System.getProperty("redicloud.console.hightlight", "§f")

    override var printingEnabled = true
    override var matchingHistorySearch = true
    @OptIn(DelicateCoroutinesApi::class)
    private val scope = CoroutineScope(newSingleThreadContext("console-scope"))

    init {
        ansiSupported = AnsiInstaller().install()

        disableJLineLogger()

        terminal = TerminalBuilder.builder()
            .system(true)
            .encoding(Charsets.UTF_8)
            .build()
        lineReader = ConsoleLineReader(this).apply {
            completer = ConsoleCompleter(this@Console.commandManager)
            highlighter = ConsoleHighlighter(this@Console)
            option(LineReader.Option.AUTO_GROUP, false)
            option(LineReader.Option.AUTO_MENU_LIST, true)
            option(LineReader.Option.AUTO_FRESH_LINE, true)
            option(LineReader.Option.EMPTY_WORD_OPTIONS, false)
            option(LineReader.Option.HISTORY_TIMESTAMPED, false)
            option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)

            variable(LineReader.BELL_STYLE, "none")
            variable(LineReader.HISTORY_SIZE, 500)
            variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "inverse")
            //TODO History file
        }

        this.updatePrompt()
        this.run()
    }

    private fun run() {
        scope.launch {
            eventManager?.fireEvent(ConsoleRunEvent(this@Console))
            var line: String? = null
            while (!Thread.currentThread().isInterrupted) {
                line = readlnOrNull() ?: continue

                commandManager.handleInput(commandManager.actor, line)

                runningAnimations.forEach { (_, animation) -> animation.addToCursorUp(1) }
            }
            fun readLine(): String? {
                try {
                    return lineReader.readLine(prompt)
                }catch (_: EndOfFileException) {
                }catch (e: UserInterruptException) {
                    exitProcess(-1)
                }
                return null
            }
        }
    }

    private fun print(text: String) {
        this.lineReader.terminal.puts(InfoCmp.Capability.carriage_return)
        this.lineReader.terminal.puts(InfoCmp.Capability.clr_eol)
        this.lineReader.terminal.writer().print(text)
        this.lineReader.terminal.flush()
        redisplay()
    }

    private fun redisplay() {
        if (!this.lineReader.isReading) return
        this.lineReader.callWidget(LineReader.REDRAW_LINE)
        this.lineReader.callWidget(LineReader.REDISPLAY)
    }

    fun formatText(input: String, ensureEndsWith: String): String {
        var content = if (ansiSupported) ConsoleColor.toColoredString('§', input) else ConsoleColor.stripColor(
            '§',
            input
        )
        if (!content.endsWith(ensureEndsWith)) {
            content += ensureEndsWith
        }
        return content
    }

    private fun disableJLineLogger() {
        java.util.logging.Logger.getLogger("org.jline").apply { level = java.util.logging.Level.OFF }
        java.util.logging.Logger.getLogger(StyleResolver::class.java.name).apply { level = java.util.logging.Level.OFF }
    }

    private fun updatePrompt() {
        this.prompt = ConsoleColor.toColoredString('§', this.prompt)
            .replace("%version%", CLOUD_VERSION)
            .replace("%user%", USER_NAME)
            .replace("%host%", this.host)
        this.lineReader.setPrompt(this.prompt)
    }

    override fun runningAnimations(): List<AbstractConsoleAnimation> = runningAnimations.values.toList()

    override fun startAnimation(animation: AbstractConsoleAnimation) {
        animation.console = this

        val uniqueId = UUID.randomUUID()
        runningAnimations[uniqueId] = animation

        scope.launch {
            animation.run()
            runningAnimations.remove(uniqueId)
            animation.console = null
            animation.handleDone();
        }
    }

    override fun animationRunning(): Boolean = runningAnimations.isNotEmpty()

    override fun getCurrentScreen(): Screen = currentScreen

    override fun switchScreen(screen: Screen) {
        if (screen == currentScreen) return
        printLock.lock()
        try {
            screen.display()
            currentScreen = screen
        }finally {
            printLock.unlock()
        }
    }

    override fun switchToDefaultScreen() {
        if (currentScreen == defaultScreen) return
        printLock.lock()
        try {
            defaultScreen.display()
            currentScreen = defaultScreen
        }finally {
            printLock.unlock()
        }
    }


    override fun commandHistory(): List<String> =
        lineReader.history.map { it.line() }.toList()

    override fun commandHistory(history: List<String>?) {
        try {
            lineReader.history.purge()
        }catch (e: IOException) {
            LOGGER.severe("Failed to purge console history", e)
        }
        history?.forEach { lineReader.history.add(it) }
    }

    override fun commandInputValue(commandInputValue: String) {
        lineReader.buffer.write(commandInputValue)
    }

    override fun enableCommands() {
        commandManager.enableCommands()
    }

    override fun disableCommands() {
        commandManager.disableCommands()
    }
    override fun writeRaw(rawText: String): Console {
        printLock.lock()
        try {
            this.print(formatText(rawText, ""))
            return this
        } finally {
            printLock.unlock()
        }
    }

    override fun forceWriteLine(text: String): Console {
        printLock.lock()
        try {
            val content = this.formatText(text, System.lineSeparator())

            if (ansiSupported) {
                this.print("${Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString()}\r$content${Ansi.ansi().reset().toString()}")
            }else {
                this.print("\r$content")
            }

            if (!runningAnimations.isEmpty()) {
                runningAnimations.values.forEach { it.addToCursorUp(1) }
            }
        }finally {
            printLock.unlock()
        }
        return this
    }

    override fun writeLine(text: String): Console {
        if (!printingEnabled) return this
        forceWriteLine(text)
        return this
    }

    override fun hasColorSupport(): Boolean = ansiSupported

    override fun resetPrompt() {
        prompt = System.getProperty("redicloud.console.promt", "%hc%%user%§8@§f%host% §8➔ §r")
        updatePrompt()
    }

    override fun removePrompt() {
        prompt = ""
        updatePrompt()
    }

    override fun emptyPrompt() {
        prompt = ConsoleColor.DEFAULT.toString()
        updatePrompt()
    }

    override fun clearScreen() {
        terminal.puts(InfoCmp.Capability.clear_screen)
        terminal.flush()
        this.redisplay()
    }

    @Throws(Exception::class)
    override fun close() {
        this.scope.cancel()
        terminal.flush()
        terminal.close()
        AnsiConsole.systemUninstall()
    }

}