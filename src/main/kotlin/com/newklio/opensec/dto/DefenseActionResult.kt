package com.newklio.opensec.dto

import com.newklio.opensec.model.DefenseActionStatus
import com.newklio.opensec.model.DefenseActionType
import java.time.Instant
import java.util.UUID

const val DEFENSE_ACTION_EXECUTED_EVENT = "defense_action_executed"

data class DefenseActionResult(
    val id: UUID = UUID.randomUUID(),
    val tenantId: UUID,
    val alertId: UUID,
    val correlationId: String? = null,
    val actionType: DefenseActionType,
    val status: DefenseActionStatus,
    val target: String,
    val detail: String,
    val dryRun: Boolean,
    val executedAt: Instant = Instant.now(),
)
