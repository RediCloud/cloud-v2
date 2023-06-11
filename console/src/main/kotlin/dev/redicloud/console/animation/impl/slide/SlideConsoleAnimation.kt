package dev.redicloud.console.animation.impl.slide

import dev.redicloud.console.Console
import dev.redicloud.console.animation.AbstractConsoleAnimation
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation

class SlideConsoleAnimation(val lines: Array<String>, updateInterval: Long, console: Console) :
    AbstractConsoleAnimation(updateInterval, false, console) {

    private val currentStrings: MutableMap<Int, String> = mutableMapOf()
    private var currentCount: Int = 0
    private val stringSize: Int
    private val animations = mutableListOf<AnimatedLineAnimation>()
    private var first = true

    init {
        stringSize = lines.maxByOrNull { it.length }?.length ?: 0
        var index = 0
        lines.forEach {
            val animation = AnimatedLineAnimation(console, updateInterval) {
                val i = index
                currentStrings[i] ?: ""
            }
            animations.add(animation)
            index++
        }
    }

    override fun handleTick(): Boolean {
        if (stringSize == 0) return true
        var index = 0
        lines.forEach {
            if (currentCount > stringSize) return true
            val currentLine = currentStrings[index] ?: ""
            val current = it.getOrNull(stringSize-currentCount)?.toString() ?: ""
            currentStrings[index] = current + currentLine
            index++
        }
        if (first) {
            animations.forEach { console.startAnimation(it) }
            first = false
        }
        currentCount++
        return false
    }


}