package com.newklio.opensec.service

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Redis-backed access-token revocation list. Each revoked `jti` is stored with a TTL equal to the
 * token's remaining lifetime, so entries self-expire and the list never grows unbounded.
 *
 * Fails open on Redis errors (treats tokens as not revoked) to preserve availability; the short
 * access-token lifetime bounds the risk window.
 */
class RedisTokenRevocationService(
    private val redisTemplate: StringRedisTemplate,
) : TokenRevocationService {
    private val log = LoggerFactory.getLogger(RedisTokenRevocationService::class.java)

    override fun revoke(
        jti: UUID,
        expiresAt: Instant,
    ) {
        val ttl = Duration.between(Instant.now(), expiresAt)
        if (ttl.isNegative || ttl.isZero) {
            return
        }
        try {
            redisTemplate.opsForValue().set(key(jti), "1", ttl)
        } catch (ex: Exception) {
            log.warn("Failed to record token revocation for jti {}: {}", jti, ex.message)
        }
    }

    override fun isRevoked(jti: UUID): Boolean =
        try {
            redisTemplate.hasKey(key(jti)) == true
        } catch (ex: Exception) {
            log.warn("Revocation check unavailable, treating token as not revoked: {}", ex.message)
            false
        }

    private fun key(jti: UUID) = "revoked:access:$jti"
}
