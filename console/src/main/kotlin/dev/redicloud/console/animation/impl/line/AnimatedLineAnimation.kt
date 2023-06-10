package dev.redicloud.console.animation.impl.line

import dev.redicloud.console.Console
import dev.redicloud.console.animation.AbstractConsoleAnimation

class AnimatedLineAnimation(console: Console, updateInterval: Long, val line: () -> String?) : AbstractConsoleAnimation(updateInterval, false, console) {

    companion object {
        private val LOADING_ANIMATION_SYMBOLS = listOf("|", "/", "-", "\\")
        private val LOADING_ANIMATION_IDENTIFIER = "%loading%"
    }

    private val loadingAnimations = mutableMapOf<Int, String>()

    override fun handleTick(): Boolean {
        val line = line()
        if (line != null) {
            if (line.contains(LOADING_ANIMATION_IDENTIFIER)) {
                val newLine = StringBuilder()
                var count = 0
                var lastIndex = 0

                while (lastIndex != -1) {
                    lastIndex = line.indexOf(LOADING_ANIMATION_IDENTIFIER, lastIndex)
                    if (lastIndex != -1) {
                        count++
                        lastIndex += LOADING_ANIMATION_IDENTIFIER.length
                    }
                }

                for (i in 0 until count) {
                    val animationState = loadingAnimations.getOrDefault(i, LOADING_ANIMATION_SYMBOLS[0])
                    val nextAnimation = replaceToNextAnimationSymbol(animationState)
                    loadingAnimations[i] = nextAnimation
                    newLine.append(line.replaceFirst(LOADING_ANIMATION_IDENTIFIER, nextAnimation))
                }

                super.print(newLine.toString())
                return false
            }
            super.print(line)
        }
        return line == null
    }

    private fun replaceToNextAnimationSymbol(currentSymbol: String): String {
        val currentIndex = LOADING_ANIMATION_SYMBOLS.indexOf(currentSymbol)
        val nextIndex = if (currentIndex + 1 >= LOADING_ANIMATION_SYMBOLS.size) 0 else currentIndex + 1
        return LOADING_ANIMATION_SYMBOLS[nextIndex]
    }

}