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

class GsonCodec : BaseCodec() {

    private var gson: Gson = GsonBuilder()
        .fixKotlinAnnotations()
        .addInterfaceImpl()
        .serializeNulls()
        .setPrettyPrinting()
        .create()

    private val encoder: Encoder = Encoder { `in`: Any ->
        val out = ByteBufAllocator.DEFAULT.buffer()
        try {
            val os = ByteBufOutputStream(out)
            val p = GsonPackage(`in`.javaClass.name, gson.toJson(`in`))
            os.writeUTF(gson.toJson(p))
            return@Encoder os.buffer()
        } catch (e: Exception) {
            out.release()
            throw e
        }
    }
    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf?, _: State? ->
        ByteBufInputStream(buf).use { stream ->
            try {
                val data = stream.readUTF()
                val p = gson.fromJson(data, GsonPackage::class.java)
                return@Decoder gson.fromJson(p.json, Class.forName(p.clazz))
            }catch (e: ClassNotFoundException) {
                return@Decoder null
            }
        }
    }

    override fun getValueEncoder() = encoder

    override fun getValueDecoder() = decoder

}

data class GsonPackage(
    val clazz: String,
    val json: String
)