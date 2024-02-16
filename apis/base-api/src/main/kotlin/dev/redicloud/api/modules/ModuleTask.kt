package dev.redicloud.api.modules

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModuleTask(val lifeCycle: ModuleLifeCycle, val order: Int = ModuleTaskOrder.DEFAULT)