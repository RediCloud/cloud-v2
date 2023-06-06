package dev.redicloud.console.events.screen

import dev.redicloud.console.events.ConsoleEvent
import dev.redicloud.console.Console
import dev.redicloud.console.utils.Screen

abstract class ScreenEvent(val screen: Screen, console: Console) : ConsoleEvent(console)