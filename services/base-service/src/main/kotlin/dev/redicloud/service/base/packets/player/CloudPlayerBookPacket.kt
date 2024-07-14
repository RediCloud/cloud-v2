package dev.redicloud.service.base.packets.player

import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.UUID

class CloudPlayerBookPacket(
    uniqueId: UUID,
    private val jsonTitle: String,
    private val jsonAuthor: String,
    private val jsonPages: List<String>
) : CloudPlayerPacket(uniqueId) {

    constructor(uniqueId: UUID, book: Book) : this(
        uniqueId,
        GsonComponentSerializer.gson().serialize(book.title()),
        GsonComponentSerializer.gson().serialize(book.author()),
        book.pages().map { GsonComponentSerializer.gson().serialize(it) }
    )

    private val title: Component
        get() = GsonComponentSerializer.gson().deserialize(jsonTitle)

    private val author: Component
        get() = GsonComponentSerializer.gson().deserialize(jsonAuthor)

    private val pages: List<Component>
        get() = jsonPages.map { GsonComponentSerializer.gson().deserialize(it) }

    fun createBook(): Book {
        return Book.book(title, author, pages)
    }

}