package dev.redicloud.service.node.console

import dev.redicloud.console.Console
import dev.redicloud.console.utils.ConsoleConfiguration
import dev.redicloud.console.utils.Design
import dev.redicloud.console.utils.LineBuilder
import dev.redicloud.utils.CLOUD_VERSION
import org.fusesource.jansi.Ansi
import kotlin.system.exitProcess

class InitializeConsole : Console(
    "initialize",
    ConsoleConfiguration(
        inputPrefix = LineBuilder.builder().text(""),
        lineFormat = LineBuilder.builder().text("")
    ),
    null) {

    init {
        sendHeader()
    }

    private fun sendHeader() {
        print {
            newLine()
                .tab().text(" _______                 __   _      ______  __                         __  ").newLine()
                .tab().text("|_   __ \\               |  ] (_)   .' ___  |[  |                       |  ] ").newLine()
                .tab().text("  | |__) |  .---.   .--.| |  __   / .'   \\_| | |  .--.   __   _    .--.| |  ").newLine()
                .tab().text("  |  __ /  / /__\\\\/ /'`\\' | [  |  | |        | |/ .'`\\ \\[  | | | / /'`\\' |  ").newLine()
                .tab().text(" _| |  \\ \\_| \\__.,| \\__/  |  | |  \\ `.___.'\\ | || \\__. | | \\_/ |,| \\__/  |  ").newLine()
                .tab().text("|____| |___|'.__.' '.__.;__][___]  `.____ .'[___]'.__.'  '.__.'_/ '.__.;__] ").newLine()
                .tab().tab().newLine()
                .tab().text("A redis based cluster cloud system for Minecraft").newLine()
                .tab().raw("»", Design(Ansi.Color.BLACK.fgBright())).space().text("Version")
                    .raw(":", Design(Ansi.Color.BLACK.fgBright())).space().highlight(CLOUD_VERSION)
                    .space().raw("|", Design(Ansi.Color.BLACK.fgBright())).space().text("Git")
                    .raw(":", Design(Ansi.Color.BLACK.fgBright())).space().highlight("------").newLine()
                .tab().raw("»", Design(Ansi.Color.BLACK.fgBright())).space().text("Discord")
                    .raw(":", Design(Ansi.Color.BLACK.fgBright())).space().highlight("https://discord.gg/g2HV52VV4G").newLine()
                .newLine()
                .newLine()
        }
    }

    override fun onExit(exception: Exception?) {
        if (exception != null) {
            exception.printStackTrace()
            exitProcess(1)
        }
    }

}