package com.newklio.opensec.repository

import com.newklio.opensec.entity.AlertRecord
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AlertRepository : JpaRepository<AlertRecord, UUID> {
    fun findByTenantId(tenantId: UUID): List<AlertRecord>

    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): AlertRecord?
}
