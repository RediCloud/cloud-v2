package dev.redicloud.api.events

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CloudEventListener(
    val priority: Int = EventPriority.NORMAL
)