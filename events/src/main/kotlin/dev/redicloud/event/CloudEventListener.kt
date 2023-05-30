package dev.redicloud.event

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CloudEventListener(
    val priority: Int = EventPriority.NORMAL
)