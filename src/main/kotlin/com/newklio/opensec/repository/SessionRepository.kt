package com.newklio.opensec.repository

import com.newklio.opensec.entity.Session
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.SessionStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SessionRepository : JpaRepository<Session, UUID> {
    fun findFirstByTenantIdAndEntityTypeAndEntityIdAndStatusOrderByLastEventAtDesc(
        tenantId: UUID,
        entityType: EntityType,
        entityId: String,
        status: SessionStatus,
    ): Session?
}
