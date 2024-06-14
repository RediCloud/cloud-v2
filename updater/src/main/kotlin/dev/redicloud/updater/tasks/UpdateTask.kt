package dev.redicloud.updater.tasks

import dev.redicloud.console.Console
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.codec.GsonPackage
import dev.redicloud.updater.BuildInfo
import dev.redicloud.updater.UpdateInfo
import dev.redicloud.utils.gson.gson
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.redisson.api.RBucket
import org.redisson.client.codec.BaseCodec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import java.nio.charset.Charset
import kotlin.reflect.KClass

abstract class UpdateTask(
    val from: BuildInfo,
    val to: BuildInfo,
    val priority: Int = 50
) {

    companion object {
        val LATEST = BuildInfo(
            "dev",
            -2,
            "-2"
        )
    }

    abstract fun prepareUpdate(updateInfo: UpdateInfo, console: Console, databaseConnection: DatabaseConnection)

    abstract fun preUpdate(updateInfo: UpdateInfo, console: Console, databaseConnection: DatabaseConnection)

    abstract fun postUpdate(updateInfo: UpdateInfo, console: Console, databaseConnection: DatabaseConnection)

    fun getBucketsByClass(clazz: KClass<*>, databaseConnection: DatabaseConnection): List<RBucket<GsonPackage>> {
        val client = databaseConnection.client
        val keys = client.keys.getKeysByPattern("cloud:*")
        val list = mutableListOf<RBucket<GsonPackage>>()
        for (key in keys) {
            val bucket = client.getBucket<GsonPackage>(key)
            val value = bucket.get()
            if (value != null && value.clazz == clazz.qualifiedName) {
                list.add(bucket)
            }
        }
        return list
    }

}

class SafeCodec : BaseCodec() {

    private val charset: Charset = Charsets.UTF_8

    private val encoder: Encoder = Encoder { `in`: Any ->
        val out = ByteBufAllocator.DEFAULT.buffer()
        try {
            if (`in` !is GsonPackage) throw IllegalArgumentException("Input is not a DatabaseGsonPackage")
            out.writeCharSequence(gson.toJson(`in`), charset)
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
            return@Decoder p
        }catch (e: ClassNotFoundException) {
            return@Decoder null
        }
    }

    override fun getValueEncoder() = encoder

    override fun getValueDecoder() = decoder

}