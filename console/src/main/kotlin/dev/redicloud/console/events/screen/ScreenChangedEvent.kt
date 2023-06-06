package dev.redicloud.console.events.screen

import dev.redicloud.console.Console
import dev.redicloud.console.utils.Screen

class ScreenChangedEvent(console: Console, screen: Screen, val oldScreen: Screen) : ScreenEvent(screen, console)