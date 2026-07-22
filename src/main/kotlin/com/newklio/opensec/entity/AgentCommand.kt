package com.newklio.opensec.entity

import com.newklio.opensec.model.AgentCommandStatus
import com.newklio.opensec.model.AgentCommandType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "agent_commands")
data class AgentCommand(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "agent_id", nullable = false)
    val agentId: UUID,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val type: AgentCommandType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AgentCommandStatus = AgentCommandStatus.PENDING,
    @Column
    val script: String? = null,
    @Column(length = 2000)
    val parameters: String? = null,
    @Column(length = 2000)
    var message: String? = null,
    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: Instant? = null,
)
