package dev.redicloud.database.codec

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import dev.redicloud.utils.gson.gson
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.redisson.client.codec.BaseCodec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import java.nio.charset.Charset

/**
 * Redisson codec that uses Gson for serialization and deserialization.
 *
 * Each value is wrapped in a [GsonPackage] containing the fully-qualified class name
 * and the JSON representation, enabling type-safe deserialization without prior
 * knowledge of the stored type.
 */
object GsonCodec : BaseCodec() {

    private val charset: Charset = Charsets.UTF_8

    @Suppress("TooGenericExceptionCaught")
    private val encoder: Encoder = Encoder { `in`: Any ->
        val out = ByteBufAllocator.DEFAULT.buffer()
        try {
            val json = gson.toJson(`in`)
            val p = GsonPackage(`in`.javaClass.name, JsonParser.parseString(json))
            out.writeCharSequence(gson.toJson(p), charset)
            return@Encoder out
        } catch (e: Exception) {
            out.release()
            throw e
        }
    }

    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, _: State ->
        try {
            val str = buf.toString(charset)
            val p = gson.fromJson(str, GsonPackage::class.java)
            return@Decoder gson.fromJson(p.json, Class.forName(p.clazz))
        } catch (_: ClassNotFoundException) {
            return@Decoder null
        }
    }

    override fun getValueEncoder() = encoder

    override fun getValueDecoder() = decoder
}

/**
 * Wrapper that pairs a serialized JSON value with its originating class name
 * for type-safe deserialization by [GsonCodec].
 *
 * @property clazz the fully-qualified class name of the serialized object
 * @property json the JSON representation of the object
 */
data class GsonPackage(
    val clazz: String,
    val json: JsonElement
)
