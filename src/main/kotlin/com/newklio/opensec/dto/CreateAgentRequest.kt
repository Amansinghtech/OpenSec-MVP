package com.newklio.opensec.dto

import com.newklio.opensec.model.AgentPlatform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateAgentRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,
    @field:Size(max = 255)
    val hostname: String? = null,
    val platform: AgentPlatform = AgentPlatform.OTHER,
    @field:Size(max = 255)
    val wazuhAgentGroup: String? = null,
    @field:Size(max = 64)
    val wazuhAgentId: String? = null,
)
