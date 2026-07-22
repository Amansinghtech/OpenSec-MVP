package com.newklio.opensec.service

import com.newklio.opensec.dto.AgentCommandRequest
import com.newklio.opensec.dto.AgentCommandResponse
import com.newklio.opensec.entity.AgentCommand
import com.newklio.opensec.model.AgentCommandStatus
import com.newklio.opensec.repository.AgentCommandRepository
import com.newklio.opensec.repository.AgentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AgentCommandService(
    private val agentRepository: AgentRepository,
    private val agentCommandRepository: AgentCommandRepository,
    private val wazuhCommandAdapter: WazuhCommandAdapter,
) {
    @Transactional
    fun dispatch(
        agentId: UUID,
        tenantId: UUID,
        request: AgentCommandRequest,
    ): AgentCommandResponse {
        val agent =
            agentRepository.findByIdAndTenantId(agentId, tenantId)
                ?: throw IllegalArgumentException("Agent not found")

        val command =
            agentCommandRepository.save(
                AgentCommand(
                    agentId = agent.id!!,
                    tenantId = tenantId,
                    type = request.type,
                    script = request.script,
                    parameters = request.parameters,
                ),
            )

        val result =
            wazuhCommandAdapter.dispatch(
                wazuhAgentId = agent.wazuhAgentId,
                type = request.type,
                script = request.script,
                parameters = request.parameters,
            )

        command.status = if (result.success) AgentCommandStatus.DISPATCHED else AgentCommandStatus.FAILED
        command.message = result.message
        val saved = agentCommandRepository.save(command)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun listCommands(
        agentId: UUID,
        tenantId: UUID,
    ): List<AgentCommandResponse> = agentCommandRepository.findByAgentIdAndTenantId(agentId, tenantId).map { it.toResponse() }

    private fun AgentCommand.toResponse() =
        AgentCommandResponse(
            id = id!!,
            agentId = agentId,
            type = type,
            status = status,
            message = message,
            createdAt = createdAt,
        )
}

data class WazuhCommandResult(
    val success: Boolean,
    val message: String,
)

interface WazuhCommandAdapter {
    fun dispatch(
        wazuhAgentId: String?,
        type: com.newklio.opensec.model.AgentCommandType,
        script: String?,
        parameters: String?,
    ): WazuhCommandResult
}

@Service
class NoopWazuhCommandAdapter : WazuhCommandAdapter {
    override fun dispatch(
        wazuhAgentId: String?,
        type: com.newklio.opensec.model.AgentCommandType,
        script: String?,
        parameters: String?,
    ): WazuhCommandResult =
        WazuhCommandResult(
            success = wazuhAgentId != null,
            message =
                if (wazuhAgentId != null) {
                    "Dispatched $type to Wazuh agent $wazuhAgentId (script=$script)"
                } else {
                    "Agent has no wazuhAgentId; command queued for manual execution"
                },
        )
}
