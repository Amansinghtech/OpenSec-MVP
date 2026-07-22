package com.newklio.opensec.service

/**
 * Simple fixed-window rate limiter abstraction. Returns true when the request identified by
 * [key] is within the configured budget, false when it should be throttled.
 */
interface RateLimiter {
    fun allow(key: String): Boolean
}
