package project.io.app.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import project.io.app.core.user.application.service.UserInfoCache
import project.io.app.core.user.domain.User
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.OutputStreamWriter
import java.util.Base64
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("[IntegrationTest] 압축 통합 테스트")
class CompressionIntegrationTest {

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, ByteArray>

    @Autowired
    private lateinit var stringRedisTemplate: RedisTemplate<String, String>

    private val objectMapper = ObjectMapper()

    @Test
    @DisplayName("ByteArray로 직렬화/압축 하면, String으로 직렬화/압축 할 때 보다 크기가 작다.")
    fun whenUseByteArrayThenSizeShouldBeSmaller() {
        val users = (1L..10_000L)
            .map { User(it, it.toString(), it % 2 == 0L) }
            .map { UserInfoCache(it) }

        val serializedObj = objectMapper.writeValueAsString(users)
        val compressedAsString = compressAsString(serializedObj)
        val compressedAsByteArray = compressStringAsByteArray(serializedObj)
        val compressedAsByteArrayWithGzip = compressWithGzip(serialize(users))

        redisTemplate.opsForValue().set("normal:bytearray", compressedAsByteArray)
        stringRedisTemplate.opsForValue().set("normal:string", serializedObj)
        stringRedisTemplate.opsForValue().set("compressed:string", compressedAsString)
        redisTemplate.opsForValue().set("compressed:bytearray", compressedAsByteArrayWithGzip)
    }

    private fun serialize(obj: Any): ByteArray {
        ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject(obj)
            }
            return bos.toByteArray()
        }
    }

    fun compressAsString(data: String): String {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gos ->
            OutputStreamWriter(gos, UTF_8).use { writer ->
                writer.write(data)
            }
        }
        return Base64.getEncoder().encodeToString(bos.toByteArray())
    }

    private fun compressStringAsByteArray(data: String): ByteArray {
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
}
