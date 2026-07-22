package com.newklio.opensec.rag

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opensec.rag")
data class RagProperties(
    val enabled: Boolean = false,
    val topK: Int = 3,
)
