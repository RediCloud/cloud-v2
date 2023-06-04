package dev.redicloud.console.utils

import org.fusesource.jansi.Ansi

data class ConsoleConfiguration (
    var name: String = "unknown",
    var inputPrefix: LineBuilder = LineBuilder.builder().lineFormat(false)
        .raw("%user%", Design(Ansi.Color.CYAN))
        .raw("@", Design(Ansi.Color.BLACK.fgBright()))
        .raw("%version%")
        .space()
        .raw("➔", Design(Ansi.Color.BLACK.fgBright()))
        .space(),
    var lineFormat: LineBuilder = LineBuilder.builder().lineFormat(false)
        .raw("[", Design(Ansi.Color.BLACK.fgBright()))
        .raw("%time%")
        .raw("] ", Design(Ansi.Color.BLACK.fgBright()))
        .raw("%prefix%")
        .raw(": ", Design(Ansi.Color.BLACK.fgBright()))
        .raw("%message%")
        .raw(": ", Design(Ansi.Color.BLACK.fgBright())),
    var defaultDesign: Design = Design(Ansi.Color.CYAN, Ansi.Color.BLACK),
    var wordHighlight: HashMap<String, Design> = hashMapOf(
        "error" to Design(Ansi.Color.RED),
        "warn" to Design(Ansi.Color.YELLOW),
        "info" to Design(Ansi.Color.GREEN),
        "debug" to Design(Ansi.Color.BLUE),
        "success" to Design(Ansi.Color.GREEN),
        "fail" to Design(Ansi.Color.RED),
        "fatal" to Design(Ansi.Color.RED),
        "trace" to Design(Ansi.Color.BLUE),
        "running" to Design(Ansi.Color.GREEN),
        "stopped" to Design(Ansi.Color.RED),
        "starting" to Design(Ansi.Color.YELLOW),
        "stopping" to Design(Ansi.Color.YELLOW)
    )
)