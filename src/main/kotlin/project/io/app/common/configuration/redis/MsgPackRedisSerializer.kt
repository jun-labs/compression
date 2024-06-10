package project.io.app.common.configuration.redis

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.msgpack.jackson.dataformat.MessagePackFactory
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException

class MsgPackRedisSerializer<T>(type: Class<T>) : RedisSerializer<T> {

    private val javaType: JavaType = ObjectMapper().constructType(type)

    private val objectMapper: ObjectMapper = ObjectMapper(MessagePackFactory()).apply {
        registerModule(Jdk8Module())
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    override fun deserialize(bytes: ByteArray?): T? {
        if (bytes == null || bytes.isEmpty()) {
            return null
        }
        return try {
            objectMapper.readValue(bytes, javaType)
        } catch (ex: Exception) {
            throw SerializationException("Could not read MsgPack JSON: ${ex.message}", ex)
        }
    }

    override fun serialize(value: T?): ByteArray {
        if (value == null) {
            return ByteArray(0)
        }
        return try {
            objectMapper.writeValueAsBytes(value)
        } catch (ex: Exception) {
            throw SerializationException("Could not write MsgPack JSON: ${ex.message}", ex)
        }
    }
}
