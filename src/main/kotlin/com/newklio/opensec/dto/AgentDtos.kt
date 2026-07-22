package com.newklio.opensec.dto

import com.newklio.opensec.model.AgentPlatform
import com.newklio.opensec.model.AgentStatus
import java.time.Instant
import java.util.UUID

data class AgentResponse(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val hostname: String?,
    val platform: AgentPlatform,
    val status: AgentStatus,
    val wazuhAgentGroup: String?,
    val wazuhAgentId: String?,
    val lastHeartbeatAt: Instant?,
    val createdAt: Instant?,
)

data class AgentEnrollmentResponse(
    val agent: AgentResponse,
    /** Full API key — shown once at enrollment; store it securely. */
    val apiKey: String,
)

data class HeartbeatRequest(
    @field:jakarta.validation.constraints.Size(max = 255)
    val hostname: String? = null,
    @field:jakarta.validation.constraints.Size(max = 64)
    val wazuhAgentId: String? = null,
)

data class FleetSummaryResponse(
    val total: Long,
    val active: Long,
    val pending: Long,
    val offline: Long,
    val revoked: Long,
)
