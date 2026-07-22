package com.newklio.opensec.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opensec.bootstrap")
data class BootstrapConfig(
    var defaultTenantName: String = "Default",
    var defaultTenantSlug: String = "default",
    var adminUsername: String = "admin",
    var adminPassword: String = "changeit-admin-password",
    var adminEmail: String = "admin@opensec.local",
    var adminPhone: String = "0000000000",
)
