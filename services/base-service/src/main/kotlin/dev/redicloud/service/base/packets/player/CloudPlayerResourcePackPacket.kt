package dev.redicloud.service.base.packets.player

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.net.URI
import java.util.UUID

class CloudPlayerResourcePackPacket(
    uniqueId: UUID,
    private val jsonPrompt: String?,
    private val required: Boolean,
    private val stringRequests: Map<UUID, Pair<URI, String>>
) : CloudPlayerPacket(uniqueId) {

    constructor(uniqueId: UUID, resourcePackPacket: ResourcePackRequest) : this(
        uniqueId,
        resourcePackPacket.prompt()?.let { GsonComponentSerializer.gson().serialize(it) },
        resourcePackPacket.required(),
        resourcePackPacket.packs().associate { it.id() to Pair(it.uri(), it.hash()) }
    )

    private val prompt: Component?
        get() = jsonPrompt?.let { GsonComponentSerializer.gson().deserialize(it) }

    private val requests: List<ResourcePackInfo>
        get() = stringRequests.map { (id, pair) -> ResourcePackInfo.resourcePackInfo(id, pair.first, pair.second) }

    fun createResourcePackRequest(): ResourcePackRequest {
        return ResourcePackRequest.resourcePackRequest()
            .packs(requests)
            .prompt(prompt)
            .required(required)
            .build()
    }

}