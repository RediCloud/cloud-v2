package dev.redicloud.console.events

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.console.Console

abstract class ConsoleEvent(val console: Console) : CloudEvent()