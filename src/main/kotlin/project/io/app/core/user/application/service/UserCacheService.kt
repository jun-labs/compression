package project.io.app.core.user.application.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import project.io.app.core.user.domain.User
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Service
class UserCacheService(
    private val redisTemplate: RedisTemplate<String, ByteArray>,
) {
    fun cacheUserInfo(user: User) {
        val idx = calculateIdx(user.id)
        val key = getKey(idx)
        val value = UserInfoCache(user)
        val serializedValue = serializeAndCompress(value)
        redisTemplate.opsForSet().add(key, serializedValue)
    }

    fun retrieveUserInfo(key: Int): List<UserInfoCache> {
        val redisKey = getKey(key.toLong())
        val rawDataSet = redisTemplate.opsForSet().members(redisKey)
        return rawDataSet?.map { bytes ->
            decompressAndDeserialize(bytes) as UserInfoCache
        } ?: listOf()
    }

    private fun serializeAndCompress(obj: Any): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gos ->
            ObjectOutputStream(gos).use { oos ->
                oos.writeObject(obj)
            }
        }
        return bos.toByteArray()
    }

    private fun decompressAndDeserialize(bytes: ByteArray): Any {
        return GZIPInputStream(ByteArrayInputStream(bytes)).use { gis ->
            ObjectInputStream(gis).use { it.readObject() }
        }
    }

    private fun getKey(idx: Long): String {
        return String.format("user:marketing:%s", idx)
    }

    fun calculateIdx(idx: Long): Long {
        return idx / DELIMITER
    }

    companion object {
        private const val DELIMITER = 1_000
    }
}

class UserInfoCache(
    user: User? = null,
) : Serializable {
    val id = user?.id
    val name = user?.name
    val marketing = user?.marketing

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserInfoCache

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
