package com.newklio.opensec.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opensec.opensearch")
data class OpenSearchProperties(
    val enabled: Boolean = false,
    val host: String = "localhost",
    val port: Int = 9200,
    val scheme: String = "http",
)
