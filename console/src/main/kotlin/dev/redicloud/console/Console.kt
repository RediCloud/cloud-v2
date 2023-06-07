package dev.redicloud.console

import dev.redicloud.console.animation.AbstractConsoleAnimation
import dev.redicloud.console.commands.ConsoleCommandManager
import dev.redicloud.console.events.ConsoleRunEvent
import dev.redicloud.console.jline.*
import dev.redicloud.console.utils.AnsiInstaller
import dev.redicloud.console.utils.ColoredConsoleLogFormatter
import dev.redicloud.console.utils.ConsoleColor
import dev.redicloud.console.utils.Screen
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.logging.LogOutputStream
import dev.redicloud.logging.Logger
import dev.redicloud.logging.clearHandlers
import dev.redicloud.logging.handler.AcceptingLogHandler
import dev.redicloud.logging.handler.LogFileHandler
import dev.redicloud.logging.handler.LogFormatter
import dev.redicloud.logging.handler.ThreadRecordDispatcher
import dev.redicloud.utils.CLOUD_VERSION
import dev.redicloud.utils.CONSOLE_HISTORY_FILE
import dev.redicloud.utils.LOG_FOLDER
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import kotlin.system.exitProcess


open class Console(val host: String, val eventManager: EventManager?, override val saveLogToFile: Boolean = false) : IConsole {

    companion object {
        private val LOGGER: Logger = LogManager.logger(Console::class.java)
        private val DATE_FORMAT = SimpleDateFormat("HH:mm:ss.SSS")
    }

    internal val currentQuestion: ConsoleQuestion? = null
    private val ansiSupported: Boolean
    private val printLock: Lock = ReentrantLock(true)
    private val inputReader: MutableList<ConsoleInputReader> = mutableListOf()
    private val defaultScreen: Screen = Screen(this, "main")
    private var currentScreen: Screen = defaultScreen
    private val screens: MutableList<Screen> = mutableListOf(defaultScreen)
    override val commandManager = ConsoleCommandManager(this)
    override var lineFormat: String = System.getProperty("redicloud.console.lineformat", "§8[§f%time%§8] %level%§8: %tc%%message%")
    private val runningAnimations: MutableMap<UUID, AbstractConsoleAnimation> = mutableMapOf()
    internal val terminal: Terminal
    internal val lineReader: ConsoleLineReader
    override var prompt: String = System.getProperty("redicloud.console.promt", "%hc%%user%§8@§f%host% §8➔ §r")
    var highlightColor: ConsoleColor = ConsoleColor.valueOf(System.getProperty("redicloud.console.highlight", "CYAN").uppercase())
    var textColor: ConsoleColor = ConsoleColor.valueOf(System.getProperty("redicloud.console.hightlight", "WHITE").uppercase())

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
            completer = ConsoleCompleter(this@Console)
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
            variable(LineReader.HISTORY_FILE, CONSOLE_HISTORY_FILE.getFile().path)
        }

        initializeLogging()

        this.updatePrompt()
        this.run()
        clearScreen()
    }

    private fun initializeLogging() {
        val rootLogger = LogManager.rootLogger()
        val consoleFormatter = if (hasColorSupport()) {
            ColoredConsoleLogFormatter(this) { lineFormat }
        } else LogFormatter(true) { lineFormat }
        val logFileWithPattern = "${LOG_FOLDER.getFile().absolutePath}/node-%d.log"
        LOG_FOLDER.createIfNotExists()
        clearHandlers(rootLogger)
        rootLogger.level = Level.SEVERE
        rootLogger.logRecordDispatcher = ThreadRecordDispatcher(rootLogger)
        rootLogger.addHandler(AcceptingLogHandler { writeLine(it) }.withFormatter(consoleFormatter))
        if (saveLogToFile) {
            rootLogger.addHandler(LogFileHandler(logFileWithPattern, append = true).withFormatter(LogFormatter.SEPARATOR))
        }

        System.setErr(LogOutputStream.forSevere(rootLogger).toPrintStream())
        System.setOut(LogOutputStream.forInformation(rootLogger).toPrintStream())
    }

    private fun run() {
        scope.launch {
            eventManager?.fireEvent(ConsoleRunEvent(this@Console))
            var line: String? = null
            while (!Thread.currentThread().isInterrupted) {
                line = this@Console.lineReader.readLine(this@Console.prompt) ?: continue

                inputReader.forEach { it.acceptInput(line) }
                inputReader.clear()

                commandManager.handleInput(commandManager.actor, line)

                runningAnimations.forEach { (_, animation) -> animation.addToCursorUp(1) }
            }
            fun readLine(): String? {
                try {
                    return lineReader.readLine(prompt)
                } catch (_: EndOfFileException) {
                } catch (e: UserInterruptException) {
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

    fun readNextInput(): String {
        val reader = ConsoleInputReader()
        inputReader.add(reader)
        return reader.readNextInput()
    }

    fun formatText(input: String, ensureEndsWith: String, useLineFormat: Boolean = true): String {
        val l = if (useLineFormat) lineFormat.replace("%message%", input) else input
        val formatted = l
            .replace("%level%", "§fCONSOLE")
            .replace("%time%", DATE_FORMAT.format(Date()))
            .replace("%hc%", this.highlightColor.ansiCode)
            .replace("%tc%", this.textColor.ansiCode)
            .replace("%version%", CLOUD_VERSION)
            .replace("%user%", USER_NAME)
            .replace("%host%", this.host)
        var content = if (ansiSupported) ConsoleColor.toColoredString('§', formatted) else ConsoleColor.stripColor(
            '§',
            formatted
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

    fun updatePrompt() {
        this.prompt = formatText(this.prompt, "", false)
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
        } finally {
            printLock.unlock()
        }
    }

    override fun getScreens(): List<Screen> = screens.toList()

    override fun createScreen(
        name: String,
        allowedCommands: List<String>,
        storeMessages: Boolean,
        maxStoredMessages: Int,
        historySize: Int
    ): Screen {
        val screen = Screen(this, name, allowedCommands, storeMessages, maxStoredMessages, historySize)
        screens.add(screen)
        return screen
    }


    override fun switchToDefaultScreen() {
        if (currentScreen == defaultScreen) return
        printLock.lock()
        try {
            defaultScreen.display()
            currentScreen = defaultScreen
        } finally {
            printLock.unlock()
        }
    }


    override fun commandHistory(): List<String> =
        lineReader.history.map { it.line() }.toList()

    override fun commandHistory(history: List<String>?) {
        try {
            lineReader.history.purge()
        } catch (e: IOException) {
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
            currentScreen.addLine(text)
            val content = this.formatText(textColor.ansiCode + text, System.lineSeparator())

            if (ansiSupported) {
                this.print(
                    "${Ansi.ansi().eraseLine(Ansi.Erase.ALL)}\r$content${
                        Ansi.ansi().reset()
                    }"
                )
            } else {
                this.print("\r$content")
            }

            if (runningAnimations.isNotEmpty()) {
                runningAnimations.values.forEach { it.addToCursorUp(1) }
            }
        } finally {
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