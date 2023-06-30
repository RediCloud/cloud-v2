package dev.redicloud.console.jline

import dev.redicloud.console.Console
import dev.redicloud.console.commands.ConsoleCommandManager
import dev.redicloud.utils.EasyCache
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.reader.impl.completer.StringsCompleter
import kotlin.time.Duration.Companion.seconds

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

        candidates.addAll(console.commandManager.getCompletions(console.commandManager.actor, line.line()).map { Candidate(it) })
    }

}