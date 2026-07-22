package com.newklio.opensec.repository

import com.newklio.opensec.entity.TenantConfig
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantConfigRepository : JpaRepository<TenantConfig, UUID> {
    fun findByTenantIdAndConfigKey(
        tenantId: UUID,
        configKey: String,
    ): TenantConfig?

    fun findByTenantId(tenantId: UUID): List<TenantConfig>
}
