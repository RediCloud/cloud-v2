package dev.redicloud.modules

import dev.redicloud.api.modules.ModuleLifeCycle
import kotlin.reflect.KFunction

data class ModuleTaskData(
    val function: KFunction<*>,
    val lifeCycle: ModuleLifeCycle,
    val order: Int
)