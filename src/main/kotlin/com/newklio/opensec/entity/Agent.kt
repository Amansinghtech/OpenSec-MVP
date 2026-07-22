package com.newklio.opensec.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.newklio.opensec.model.AgentPlatform
import com.newklio.opensec.model.AgentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "agents",
    indexes = [
        Index(name = "idx_agents_tenant", columnList = "tenant_id"),
        Index(name = "idx_agents_api_key_prefix", columnList = "api_key_prefix"),
    ],
)
data class Agent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(nullable = false)
    var name: String,
    @Column
    var hostname: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var platform: AgentPlatform = AgentPlatform.OTHER,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AgentStatus = AgentStatus.PENDING,
    @Column(name = "api_key_prefix", nullable = false, length = 16)
    val apiKeyPrefix: String,
    @field:JsonIgnore
    @Column(name = "api_key_hash", nullable = false)
    val apiKeyHash: String,
    @Column(name = "wazuh_agent_group")
    var wazuhAgentGroup: String? = null,
    @Column(name = "wazuh_agent_id")
    var wazuhAgentId: String? = null,
    @Column(name = "last_heartbeat_at")
    var lastHeartbeatAt: Instant? = null,
    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: Instant? = null,
)

class AgentPrincipal(
    val agent: Agent,
) {
    val tenantId: UUID get() = agent.tenantId

    val authorities: Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_AGENT"))
}
