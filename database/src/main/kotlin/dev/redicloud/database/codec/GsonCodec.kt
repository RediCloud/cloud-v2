package dev.redicloud.database.codec

import com.google.gson.*
import com.google.gson.annotations.Expose
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import org.redisson.client.codec.BaseCodec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder

class GsonCodec : BaseCodec() {

    private val gson: Gson = GsonBuilder().addSerializationExclusionStrategy(object : ExclusionStrategy {
        override fun shouldSkipField(f: FieldAttributes?): Boolean =
            f?.getAnnotation(Expose::class.java)?.serialize == false

        override fun shouldSkipClass(p0: Class<*>?): Boolean = p0?.getAnnotation(Expose::class.java)?.serialize == false
    }).addDeserializationExclusionStrategy(object : ExclusionStrategy {
        override fun shouldSkipField(f: FieldAttributes?): Boolean =
            f?.getAnnotation(Expose::class.java)?.deserialize == false

        override fun shouldSkipClass(clazz: Class<*>?): Boolean =
            clazz?.getAnnotation(Expose::class.java)?.deserialize == false
    }).setPrettyPrinting().create()

    private val encoder: Encoder = Encoder { `in`: Any ->
        val out = ByteBufAllocator.DEFAULT.buffer()
        try {
            val os = ByteBufOutputStream(out)
            val jsonElement = gson.toJsonTree(`in`)
            val jsonObject = jsonElement.asJsonObject
            jsonObject.add("class", JsonPrimitive(`in`::class.java.name))
            os.writeUTF(jsonObject.toString())
            return@Encoder os.buffer()
        } catch (e: Exception) {
            out.release()
            throw e
        }
    }
    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf?, _: State? ->
        ByteBufInputStream(buf).use { stream ->
            val json = stream.readUTF()
            val jsonObject = gson.fromJson(json, JsonObject::class.java)
            val clazz = Class.forName(jsonObject.get("class").asString)
            jsonObject.remove("class")
            return@Decoder gson.fromJson(json, clazz)
        }
    }

    override fun getValueEncoder() = encoder

    override fun getValueDecoder() = decoder

}