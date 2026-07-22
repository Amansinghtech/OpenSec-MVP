package com.newklio.opensec.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opensec.rate-limit")
data class RateLimitProperties(
    var enabled: Boolean = false,
    var limit: Int = 20,
    var windowSeconds: Long = 60,
    var pathPrefixes: List<String> = listOf("/api/v1/auth/"),
)
