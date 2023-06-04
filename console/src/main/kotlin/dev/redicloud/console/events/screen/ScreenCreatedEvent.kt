package dev.redicloud.console.events.screen

import dev.redicloud.console.Console
import dev.redicloud.console.utils.Screen

class ScreenCreatedEvent(screen: Screen, console: Console) : ScreenEvent(screen, console)