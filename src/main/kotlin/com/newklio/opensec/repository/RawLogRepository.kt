package com.newklio.opensec.repository

import com.newklio.opensec.entity.RawLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RawLogRepository : JpaRepository<RawLog, UUID> {
    fun existsByTenantIdAndSourceAndEventId(
        tenantId: UUID,
        source: String,
        eventId: String,
    ): Boolean
}
