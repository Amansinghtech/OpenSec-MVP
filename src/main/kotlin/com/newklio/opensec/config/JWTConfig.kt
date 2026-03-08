package com.newklio.opensec.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JWTConfig(
    var secret: String,
    var expiration: Long
)