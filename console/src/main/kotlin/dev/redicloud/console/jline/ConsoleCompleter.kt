package dev.redicloud.console.jline

import dev.redicloud.console.Console
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class ConsoleCompleter(val console: Console) : Completer {

    override fun complete(reader: LineReader?, line: ParsedLine?, candidates: MutableList<Candidate>?) {
        if (line == null || candidates == null) return

        if (console.currentQuestion != null) {
            val question = console.currentQuestion
            question.completer.forEach {
                candidates.add(Candidate(it))
            }
            return
        }

        candidates.addAll(console.commandManager.getCompletions(console.commandManager.defaultActor, line.line()).map { Candidate(it) })
    }

}