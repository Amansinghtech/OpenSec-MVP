package com.newklio.opensec.config

import com.newklio.opensec.service.RateLimiter
import com.newklio.opensec.service.RedisRateLimiter
import com.newklio.opensec.service.RedisTokenRevocationService
import com.newklio.opensec.service.TokenRevocationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * Wires the Redis-backed components. The `StringRedisTemplate` and connection factory are provided
 * by Spring Boot's Redis auto-configuration; these beans only activate when explicitly enabled so
 * the app (and the test suite) still runs without a Redis instance.
 */
@Configuration
class RedisConfig {
    @Bean
    @ConditionalOnProperty(name = ["opensec.revocation.store"], havingValue = "redis")
    fun redisTokenRevocationService(redisTemplate: StringRedisTemplate): TokenRevocationService = RedisTokenRevocationService(redisTemplate)

    @Bean
    @ConditionalOnProperty(name = ["opensec.rate-limit.enabled"], havingValue = "true")
    fun redisRateLimiter(
        redisTemplate: StringRedisTemplate,
        properties: RateLimitProperties,
    ): RateLimiter = RedisRateLimiter(redisTemplate, properties)
}
