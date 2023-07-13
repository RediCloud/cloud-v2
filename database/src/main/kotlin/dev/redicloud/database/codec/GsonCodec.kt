package dev.redicloud.database.codec

import com.google.gson.*
import dev.redicloud.utils.gson.addInterfaceImpl
import dev.redicloud.utils.gson.fixKotlinAnnotations
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import org.redisson.client.codec.BaseCodec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import java.nio.charset.Charset

class GsonCodec : BaseCodec() {

    private val gson: Gson = GsonBuilder()
        .addInterfaceImpl()
        .fixKotlinAnnotations()
        .serializeNulls()
        .setPrettyPrinting()
        .create()

    private val charset: Charset = Charsets.UTF_8

    private val encoder: Encoder = Encoder { `in`: Any ->
        val out = ByteBufAllocator.DEFAULT.buffer()
        try {
            val p = GsonPackage(`in`.javaClass.name, JsonParser.parseString(gson.toJson(`in`)))
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
    val json: JsonElement
)