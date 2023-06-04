package dev.redicloud.console.events

import dev.redicloud.console.Console
import dev.redicloud.event.CloudEvent

abstract class ConsoleEvent(val console: Console) : CloudEvent()