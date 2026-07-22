package com.newklio.opensec.repository

import com.newklio.opensec.entity.Agent
import com.newklio.opensec.model.AgentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AgentRepository : JpaRepository<Agent, UUID> {
    fun findByTenantId(tenantId: UUID): List<Agent>

    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Agent?

    fun existsByTenantIdAndName(
        tenantId: UUID,
        name: String,
    ): Boolean

    fun findByApiKeyPrefixAndStatusNot(
        apiKeyPrefix: String,
        status: AgentStatus,
    ): List<Agent>

    fun countByTenantIdAndStatus(
        tenantId: UUID,
        status: AgentStatus,
    ): Long
}
