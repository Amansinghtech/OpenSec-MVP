package com.newklio.opensec.repository

import com.newklio.opensec.entity.DefenseActionRecord
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DefenseActionRepository : JpaRepository<DefenseActionRecord, UUID> {
    fun findByTenantId(tenantId: UUID): List<DefenseActionRecord>
}
