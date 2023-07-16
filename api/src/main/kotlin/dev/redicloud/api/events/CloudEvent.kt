package dev.redicloud.api.events

abstract class CloudEvent(val fireType: EventFireType = EventFireType.LOCAL)