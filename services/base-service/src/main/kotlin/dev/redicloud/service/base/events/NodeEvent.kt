package dev.redicloud.service.base.events

import dev.redicloud.event.CloudEvent
import dev.redicloud.event.EventFireType
import dev.redicloud.repository.node.CloudNode

abstract class NodeEvent(val node: CloudNode, fireType: EventFireType) : CloudEvent(fireType)