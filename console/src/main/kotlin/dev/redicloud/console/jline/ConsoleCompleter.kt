package dev.redicloud.console.jline

import dev.redicloud.console.commands.ConsoleCommandManager
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class ConsoleCompleter(val commandManager: ConsoleCommandManager) : Completer {

    override fun complete(reader: LineReader?, line: ParsedLine?, candidates: MutableList<Candidate>?) {
        if (line == null || candidates == null) return

        candidates.addAll(commandManager.getCompletions(commandManager.actor, line.line()).map { Candidate(it) })
    }

}