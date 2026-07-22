package com.newklio.opensec.entity

import com.newklio.opensec.model.DefenseActionStatus
import com.newklio.opensec.model.DefenseActionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "defense_actions")
data class DefenseActionRecord(
    @Id
    val id: UUID,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "alert_id", nullable = false)
    val alertId: UUID,
    @Column(name = "correlation_id")
    val correlationId: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    val actionType: DefenseActionType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: DefenseActionStatus,
    @Column(nullable = false)
    val target: String,
    @Column(nullable = false, length = 2000)
    val detail: String,
    @Column(name = "dry_run", nullable = false)
    val dryRun: Boolean,
    @Column(name = "executed_at", nullable = false)
    val executedAt: Instant,
)
