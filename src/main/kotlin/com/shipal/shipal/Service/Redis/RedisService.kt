package com.shipal.shipal.Service.Redis

import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.SessionCallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.time.Duration

@Service
class RedisService (
    private val redis: StringRedisTemplate
)

{
    fun ping(): String? = redis.connectionFactory?.connection?.ping()

    private fun sessionKey(userSeq: Int) = "Session:login:$userSeq"
    private val rnd = SecureRandom()

    private fun newRefreshToken(): String{
        val buf = ByteArray(64)
        rnd.nextBytes(buf)

        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf)
    }

    /*
    * 리프레쉬 토큰 저장
    * */
    fun setRefreshToken(
        userSeq: Int,
        accessToken: String,
        uuid: String) : Pair<String, String>? {

        val key = sessionKey(userSeq)
        try
        {
            // 이미 로그인된 기기가 있고, UUID가 다르면 기존 세션 정리
            val existingUuid = redis.opsForHash<String, String>().get(key, "uuid")
            if(existingUuid != null && existingUuid != uuid){
                redis.delete(key)
            }

            val refreshToken = newRefreshToken()
            val fields = mapOf(
                "uuid" to uuid,
                "refreshToken" to refreshToken,
                "updated" to Instant.now().toString()
            )

            // MULTI/EXEC 트랜잭션: HSET + EXPIRE 원자 처리
            val committed = redis.execute(object : SessionCallback<List<Any>?> {
                override fun <K, V> execute(ops0: RedisOperations<K, V>): List<Any>? {
                    @Suppress("UNCHECKED_CAST")
                    val ops = ops0 as RedisOperations<String, String>
                    ops.multi()
                    ops.opsForHash<String, String>().putAll(key, fields)
                    ops.expire(key, Duration.ofDays(3))
                    return ops.exec()
                }
            }) != null

            return if(committed) Pair(accessToken, refreshToken) else null
        }
        catch (e: DataAccessException)
        {
            return null;
        }
        catch (e: Exception){
            return null
        }
    }

    /*
    * 리프레쉬 토큰 재발급(회전)
    * 남은 TTL < 1 시간이면 새 토큰으로 교체, TTL은 항상 갱신
    * */
    fun rotateRefreshToken(userSeq: Int, refreshToken: String, uuid: String) : String? {
        val key = sessionKey(userSeq)

        try{
            // 로그인 된 디바이스 검증
            var existingUuid = redis.opsForHash<String, String>().get(key, "uuid") ?: return null

            if (existingUuid != uuid) return null

            // 저장된 토큰 검증
            val storedToken = redis.opsForHash<String, String>().get(key, "refreshToken") ?: return null
            if (storedToken != refreshToken) return null

            // 남은 TTL 확인(초)
            val ttlSec = redis.getExpire(key, TimeUnit.SECONDS) ?: return null
            if (ttlSec <= 0L) return null

            var returnToken = refreshToken
            val needRotate = ttlSec < 3600 // 1시간 미만이면 회전

            val committed = redis.execute(object : SessionCallback<List<Any>?> {
                @Suppress("UNCHECKED_CAST")
                override fun <K, V> execute(operations: RedisOperations<K, V>): List<Any>? {
                    val ops = operations as RedisOperations<String, String>
                    val rotated = if (needRotate) newRefreshToken() else refreshToken
                    ops.multi()
                    if (needRotate) {
                        ops.opsForHash<String, String>().putAll(
                            key,
                            mapOf(
                                "refreshToken" to rotated,
                                "updated" to Instant.now().toString()
                            )
                        )
                        returnToken = rotated
                    } else {
                        // 회전은 안 하지만 업데이트 시각만 남기고 싶다면 주석 해제
                        // ops.opsForHash<String, String>().put(key, "updated", Instant.now().toString())
                    }
                    ops.expire(key, Duration.ofDays(3))
                    return ops.exec()
                }
            }) != null

            return if (committed) returnToken else null
        }
        catch (e: Exception){
            return null
        }
    }

    /*
    * 리프레시 토큰 조회
    * */
    fun getRefreshToken(userSeq: Int, uuid: String): String? {
        val key = sessionKey(userSeq)
        return try {
            val existingUuid = redis.opsForHash<String, String>().get(key, "uuid") ?: return null
            if (existingUuid != uuid) return null
            redis.opsForHash<String, String>().get(key, "refreshToken")
        } catch (e: Exception) {
            null
        }
    }


    /**
     * 세션 전체 조회 (Hash → Map<String, String>)
     * .NET: GetSessionAsync
     */
    fun getSession(userSeq: Int): Map<String, String>? {
        val key = sessionKey(userSeq)
        return try {
            if (redis.hasKey(key) != true) return null
            redis.opsForHash<String, String>().entries(key).mapKeys { it.key.toString() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 로그아웃: 세션 키 삭제
     * .NET: LogOutTokenAsync
     */
    fun logout(userSeq: Int): Boolean {
        val key = sessionKey(userSeq)
        return try {
            redis.delete(key)
        } catch (e: Exception) {
            false
        }
    }
}

