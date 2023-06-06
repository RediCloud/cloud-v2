package dev.redicloud.console.utils

import dev.redicloud.console.Console
import org.fusesource.jansi.Ansi
import java.text.SimpleDateFormat

class LineBuilder(private val consoleConfiguration: ConsoleConfiguration?) {

    private val entries = mutableListOf<String>()

    private var defaultDesign: Design = consoleConfiguration?.defaultDesign ?: Design(Ansi.Color.WHITE, Ansi.Color.BLACK)
    private var highlightDesign: Design = Design(Ansi.Color.CYAN, Ansi.Color.BLACK)
    private var lineFormat: Boolean = true
    private var timeFormat: SimpleDateFormat = SimpleDateFormat("HH:mm:ss:SSS")
    private var source: LineSource = LineSource.CUSTOM
    private var prefix: String = "INFO"

    fun getContent(): String {
        val content = entries.joinToString("")
        if (content.endsWith("\n")) return content
        return "$content\n"
    }

    fun prefix(prefix: String): LineBuilder {
        this.prefix = prefix
        return this
    }

    fun source(source: LineSource): LineBuilder {
        this.source = source
        if (source == LineSource.COMMAND) prefix = "COMMAND"
        return this
    }

    fun lineFormat(lineFormat: Boolean): LineBuilder {
        this.lineFormat = lineFormat
        return this
    }

    fun throwable(throwable: Throwable): LineBuilder {
        entries.addAll(stacktraceToString(throwable).split("\n"))
        return this
    }

    fun defaultDesign(design: Design): LineBuilder {
        defaultDesign = design
        return this
    }

    fun highlightDesign(design: Design): LineBuilder {
        highlightDesign = design
        return this
    }

    fun space(): LineBuilder {
        entries.add(" ")
        return this
    }

    fun text(text: String): LineBuilder {
        entries.add(defaultDesign.apply(text))
        return this
    }

    fun highlight(text: String): LineBuilder {
        entries.add(highlightDesign.apply(text))
        return this
    }

    fun raw(text: String, design: Design? = null): LineBuilder {
        if (design != null) {
            entries.add(design.apply(text))
        } else {
            entries.add(text)
        }
        return this
    }

    fun newLine(): LineBuilder {
        entries.add("\n")
        return this
    }

    fun tab(): LineBuilder {
        entries.add("\t")
        return this
    }

    fun replace(identifier: String, replacement: String) {
        entries.replaceAll { it.replace(identifier, replacement) }
    }

    companion object {
        var configuration: ConsoleConfiguration = ConsoleConfiguration()

        fun builder(console: Console? = null): LineBuilder = LineBuilder(console?.configuration ?: configuration)
        fun rawBuilder(): LineBuilder = LineBuilder(null)
    }

}

enum class LineSource() {
    CUSTOM,
    COMMAND,
    LOG
}