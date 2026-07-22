package com.newklio.opensec.service

import com.newklio.opensec.config.RateLimitProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

/**
 * Fixed-window rate limiter backed by Redis: `INCR` the counter for the window and set a TTL on
 * first hit. Shared across instances, so it works behind a horizontally scaled gateway.
 *
 * Fails open: if Redis is unavailable the request is allowed through rather than blocking auth.
 */
class RedisRateLimiter(
    private val redisTemplate: StringRedisTemplate,
    private val properties: RateLimitProperties,
) : RateLimiter {
    private val log = LoggerFactory.getLogger(RedisRateLimiter::class.java)

    override fun allow(key: String): Boolean =
        try {
            val count = redisTemplate.opsForValue().increment(key) ?: 1L
            if (count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(properties.windowSeconds))
            }
            count <= properties.limit
        } catch (ex: Exception) {
            log.warn("Rate limiter unavailable, allowing request: {}", ex.message)
            true
        }
}
