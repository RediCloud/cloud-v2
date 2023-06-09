package dev.redicloud.console.animation.impl.line

import dev.redicloud.console.Console
import dev.redicloud.console.animation.AbstractConsoleAnimation

class AnimatedLineAnimation(console: Console, val line: () -> String?) : AbstractConsoleAnimation(500, false, console) {

    override fun handleTick(): Boolean {
        val line = line()
        if (line != null) super.print(line)
        return line == null
    }

}