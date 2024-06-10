package project.io.app.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.luben.zstd.ZstdInputStream
import com.github.luben.zstd.ZstdOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import project.io.app.common.configuration.redis.MsgPackRedisSerializer
import project.io.app.common.configuration.redis.SnappyMsgPackRedisSerializer
import project.io.app.core.user.application.service.UserInfoCache
import project.io.app.core.user.domain.User
import project.io.app.logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStreamWriter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

@Suppress("UNCHECKED_CAST")
@DisplayName("[UnitTest] 압축 단위 테스트")
class SerializationCompressionTest {

    private val log = logger()
    private val objectMapper = jacksonObjectMapper()

    @Test
    @DisplayName("압축을 하면 데이터 크기가 작아진다.")
    fun whenCompressThenSizeShouldBeSmaller() {
        val users = (1L..100_000L)
            .map { User(it, it.toString(), false) }
            .map { UserInfoCache(it) }

        val uncompressed = serialize(users)
        val snappyCompressed = compressWithSnappy(users)
        val msgPackSerialized = serializeWithMsgPack(users)
        val gzipCompressed = compressWithGzip(users)
        val zstdCompressed = compressWithZstd(users)

        val snappyDecompressed = deserializeAndUnCompressWithSnappy(snappyCompressed)
        val msgPackDecompressed = deserializeWithMsgPack(msgPackSerialized)
        val gzipDecompressed = deserializeAndDecompressWithGZip(gzipCompressed)
        val zstdDecompressed = deserializeAndDecompressWithZstd(zstdCompressed)

        log.info("[Uncompressed] size: ${convertMB(uncompressed.size)} MB")
        log.info("[MsgPack Serialized] size: ${convertMB(msgPackSerialized.size)} MB")
        log.info("[Snappy Compressed] size: ${convertMB(snappyCompressed.size)} MB")
        log.info("[GZIP Compressed] size: ${convertMB(gzipCompressed.size)} MB")
        log.info("[Zstd Compressed] size: ${convertMB(zstdCompressed.size)} MB")

        assertEquals(users.size, snappyDecompressed.size)
        assertEquals(users.size, msgPackDecompressed.size)
        assertEquals(users.size, (gzipDecompressed as List<*>).size)
        assertEquals(users.size, (zstdDecompressed as List<*>).size)
    }

    @Test
    @DisplayName("너무 작은 데이터를 압축하면, 오버헤드가 발생할 수 있다.")
    fun whenCompressTooSmallDataThenSizeShouldBeBigger() {
        val id = 1L

        val uncompressed = serialize(id)
        val compressed = compressWithGzip(id)

        log.info("[Uncompressed] size: ${uncompressed.size} byte")
        log.info("[GZIP Compressed] size: ${compressed.size} byte")
    }

    private fun serialize(obj: Any): ByteArray {
        ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject(obj)
            }
            return bos.toByteArray()
        }
    }

    private fun compressWithSnappy(obj: List<UserInfoCache>): ByteArray {
        val serializer = SnappyMsgPackRedisSerializer(List::class.java)
        return serializer.serialize(obj)
    }

    private fun deserializeAndUnCompressWithSnappy(data: ByteArray): List<UserInfoCache> {
        val serializer = SnappyMsgPackRedisSerializer(List::class.java)
        return serializer.deserialize(data) as List<UserInfoCache>
    }

    private fun serializeWithMsgPack(obj: List<UserInfoCache>): ByteArray {
        val serializer = MsgPackRedisSerializer(List::class.java)
        return serializer.serialize(obj)
    }

    private fun deserializeWithMsgPack(data: ByteArray): List<UserInfoCache> {
        val serializer = MsgPackRedisSerializer(List::class.java)
        return serializer.deserialize(data) as List<UserInfoCache>
    }

    private fun compressWithGzip(obj: Any): ByteArray {
        ByteArrayOutputStream().use { bos ->
            GZIPOutputStream(bos).use { gos ->
                ObjectOutputStream(gos).use { oos ->
                    oos.writeObject(obj)
                }
            }
            return bos.toByteArray()
        }
    }

    private fun deserializeAndDecompressWithGZip(data: ByteArray): Any {
        ByteArrayInputStream(data).use { bais ->
            GZIPInputStream(bais).use { gis ->
                ObjectInputStream(gis).use { ois ->
                    return ois.readObject()
                }
            }
        }
    }

    private fun compressWithZstd(obj: Any): ByteArray {
        ByteArrayOutputStream().use { bos ->
            ZstdOutputStream(bos).use { zos ->
                ObjectOutputStream(zos).use { oos ->
                    oos.writeObject(obj)
                }
            }
            return bos.toByteArray()
        }
    }

    private fun deserializeAndDecompressWithZstd(data: ByteArray): Any {
        ByteArrayInputStream(data).use { bais ->
            ZstdInputStream(bais).use { zis ->
                ObjectInputStream(zis).use { ois ->
                    return ois.readObject()
                }
            }
        }
    }

    @Test
    @DisplayName("ByteArray로 직렬화 하면, Json 형태로 직렬화할 때 보다 데이터 크기가 작다.")
    fun whenByteArrayCompressedThenSizeShouldBeSmallerThanJson() {
        val users = (1L..10_000L)
            .map { User(it, it.toString(), it % 2 == 0L) }
            .map { UserInfoCache(it) }

        val serializedObj = objectMapper.writeValueAsString(users)
        val compressedObj = compressToStringAsByteArray(serializedObj)

        val byteArray = serialize(users)
        val compressedWithGzip = compressWithGzip(byteArray)

        log.info("[Json String] size: ${serializedObj.toByteArray().size} byte")
        log.info("[Compressed Json String] size: ${compressedObj.size} byte")
        log.info("[Compressed ByteArray with GZip] size: ${compressedWithGzip.size} byte")

        assertTrue(compressedObj.size > compressedWithGzip.size)
    }

    private fun compressToStringAsByteArray(data: String): ByteArray {
        ByteArrayOutputStream().use { bos ->
            GZIPOutputStream(bos).use { gos ->
                val writer = OutputStreamWriter(gos, UTF_8)
                writer.write(data)
                writer.close()
            }
            return bos.toByteArray()
        }
    }

    private fun compressWithGzip(data: ByteArray): ByteArray {
        ByteArrayOutputStream().use { bos ->
            GZIPOutputStream(bos).use { gos ->
                gos.write(data)
            }
            return bos.toByteArray()
        }
    }

    private fun convertMB(byte: Int): String {
        return String.format("%.2f", byte / BYTE_TO_MB)
    }

    companion object {
        private const val BYTE_TO_MB = 1048576.0
    }
}
