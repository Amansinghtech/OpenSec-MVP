package com.newklio.opensec.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opensec.defense")
data class DefenseProperties(
    val enabled: Boolean = true,
    val dryRun: Boolean = true,
    val minRiskScore: Int = 80,
)
