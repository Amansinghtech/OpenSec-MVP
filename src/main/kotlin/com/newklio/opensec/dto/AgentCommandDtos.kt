package com.newklio.opensec.dto

import com.newklio.opensec.model.AgentCommandStatus
import com.newklio.opensec.model.AgentCommandType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class AgentCommandRequest(
    val type: AgentCommandType,
    @field:NotBlank
    @field:Size(max = 255)
    val script: String? = null,
    @field:Size(max = 2000)
    val parameters: String? = null,
)

data class AgentCommandResponse(
    val id: UUID,
    val agentId: UUID,
    val type: AgentCommandType,
    val status: AgentCommandStatus,
    val message: String?,
    val createdAt: Instant?,
)
