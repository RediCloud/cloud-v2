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

object GsonCodec : BaseCodec() {

    private val charset: Charset = Charsets.UTF_8

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
        }catch (e: ClassNotFoundException) {
            return@Decoder null
        }
    }

    override fun getValueEncoder() = encoder

    override fun getValueDecoder() = decoder

}

data class GsonPackage(
    val clazz: String,
    var json: JsonElement
)