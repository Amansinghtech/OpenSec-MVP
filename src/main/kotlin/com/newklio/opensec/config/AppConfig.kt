package com.newklio.opensec.config

import com.newklio.opensec.rag.RagProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    JWTConfig::class,
    BootstrapConfig::class,
    RateLimitProperties::class,
    SessionProperties::class,
    DetectionProperties::class,
    OpenSearchProperties::class,
    RagProperties::class,
    DefenseProperties::class,
)
class AppConfig
