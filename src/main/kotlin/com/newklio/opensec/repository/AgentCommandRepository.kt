package com.newklio.opensec.repository

import com.newklio.opensec.entity.AgentCommand
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AgentCommandRepository : JpaRepository<AgentCommand, UUID> {
    fun findByAgentIdAndTenantId(
        agentId: UUID,
        tenantId: UUID,
    ): List<AgentCommand>
}
