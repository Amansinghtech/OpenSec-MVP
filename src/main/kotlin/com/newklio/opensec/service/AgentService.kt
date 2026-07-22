package com.newklio.opensec.service

import com.newklio.opensec.dto.AgentEnrollmentResponse
import com.newklio.opensec.dto.AgentResponse
import com.newklio.opensec.dto.CreateAgentRequest
import com.newklio.opensec.dto.FleetSummaryResponse
import com.newklio.opensec.dto.HeartbeatRequest
import com.newklio.opensec.entity.Agent
import com.newklio.opensec.model.AgentStatus
import com.newklio.opensec.repository.AgentRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

const val AGENT_API_KEY_PREFIX = "opsk_"
const val AGENT_API_KEY_PREFIX_LENGTH = 12
private val AGENT_OFFLINE_THRESHOLD: Duration = Duration.ofMinutes(15)

@Service
class AgentService(
    private val agentRepository: AgentRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun enroll(
        request: CreateAgentRequest,
        tenantId: UUID,
    ): AgentEnrollmentResponse {
        if (agentRepository.existsByTenantIdAndName(tenantId, request.name)) {
            throw IllegalStateException("An agent named '${request.name}' already exists for this tenant")
        }

        val apiKey = generateApiKey()
        val agent =
            agentRepository.save(
                Agent(
                    tenantId = tenantId,
                    name = request.name,
                    hostname = request.hostname,
                    platform = request.platform,
                    status = AgentStatus.PENDING,
                    apiKeyPrefix = apiKey.take(AGENT_API_KEY_PREFIX_LENGTH),
                    apiKeyHash = passwordEncoder.encode(apiKey)!!,
                    wazuhAgentGroup = request.wazuhAgentGroup,
                    wazuhAgentId = request.wazuhAgentId,
                ),
            )

        return AgentEnrollmentResponse(agent = agent.toResponse(), apiKey = apiKey)
    }

    @Transactional(readOnly = true)
    fun listAgents(tenantId: UUID): List<AgentResponse> = agentRepository.findByTenantId(tenantId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getAgent(
        id: UUID,
        tenantId: UUID,
    ): AgentResponse? = agentRepository.findByIdAndTenantId(id, tenantId)?.toResponse()

    @Transactional
    fun revoke(
        id: UUID,
        tenantId: UUID,
    ): Boolean {
        val agent =
            agentRepository.findByIdAndTenantId(id, tenantId)
                ?: return false
        agent.status = AgentStatus.REVOKED
        agentRepository.save(agent)
        return true
    }

    @Transactional
    fun heartbeat(
        agent: Agent,
        request: HeartbeatRequest,
    ): AgentResponse {
        request.hostname?.let { agent.hostname = it }
        request.wazuhAgentId?.let { agent.wazuhAgentId = it }
        agent.lastHeartbeatAt = Instant.now()
        agent.status = AgentStatus.ACTIVE
        return agentRepository.save(agent).toResponse()
    }

    @Transactional(readOnly = true)
    fun fleetSummary(tenantId: UUID): FleetSummaryResponse {
        val agents = agentRepository.findByTenantId(tenantId)
        return FleetSummaryResponse(
            total = agents.size.toLong(),
            active = agents.count { effectiveStatus(it) == AgentStatus.ACTIVE }.toLong(),
            pending = agents.count { it.status == AgentStatus.PENDING }.toLong(),
            offline = agents.count { effectiveStatus(it) == AgentStatus.OFFLINE }.toLong(),
            revoked = agents.count { it.status == AgentStatus.REVOKED }.toLong(),
        )
    }

    @Transactional(readOnly = true)
    fun authenticateByApiKey(apiKey: String): Agent? {
        if (!apiKey.startsWith(AGENT_API_KEY_PREFIX) || apiKey.length < AGENT_API_KEY_PREFIX_LENGTH) {
            return null
        }
        val prefix = apiKey.take(AGENT_API_KEY_PREFIX_LENGTH)
        val candidates = agentRepository.findByApiKeyPrefixAndStatusNot(prefix, AgentStatus.REVOKED)
        return candidates.firstOrNull { passwordEncoder.matches(apiKey, it.apiKeyHash) }
    }

    private fun effectiveStatus(agent: Agent): AgentStatus {
        if (agent.status == AgentStatus.ACTIVE &&
            agent.lastHeartbeatAt != null &&
            Duration.between(agent.lastHeartbeatAt, Instant.now()) > AGENT_OFFLINE_THRESHOLD
        ) {
            return AgentStatus.OFFLINE
        }
        return agent.status
    }

    private fun generateApiKey(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        return AGENT_API_KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun Agent.toResponse() =
        AgentResponse(
            id = id!!,
            tenantId = tenantId,
            name = name,
            hostname = hostname,
            platform = platform,
            status = effectiveStatus(this),
            wazuhAgentGroup = wazuhAgentGroup,
            wazuhAgentId = wazuhAgentId,
            lastHeartbeatAt = lastHeartbeatAt,
            createdAt = createdAt,
        )
}
