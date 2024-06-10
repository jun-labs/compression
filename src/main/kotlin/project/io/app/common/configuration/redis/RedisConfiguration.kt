package project.io.app.common.configuration.redis

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfiguration {

    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, ByteArray> {
        val template = RedisTemplate<String, ByteArray>()
        template.connectionFactory = redisConnectionFactory
        template.keySerializer = StringRedisSerializer()

        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = ByteArrayRedisSerializer()

        return template
    }
}

class ByteArrayRedisSerializer : RedisSerializer<ByteArray> {
    @Throws(SerializationException::class)
    override fun serialize(byteArray: ByteArray?): ByteArray? {
        return byteArray
    }

    @Throws(SerializationException::class)
    override fun deserialize(byteArray: ByteArray?): ByteArray? {
        return byteArray
    }
}
