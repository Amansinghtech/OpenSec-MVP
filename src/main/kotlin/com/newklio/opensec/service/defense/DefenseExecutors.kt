package com.newklio.opensec.service.defense

import com.newklio.opensec.dto.Alert
import com.newklio.opensec.model.DefenseActionType
import org.springframework.stereotype.Component

@Component
class BlockIpExecutor : DefenseExecutor {
    override val actionType = DefenseActionType.BLOCK_IP

    override fun execute(
        alert: Alert,
        dryRun: Boolean,
    ): String = if (dryRun) "Would block IP for ${alert.entityId}" else "Blocked IP for ${alert.entityId}"
}

@Component
class RevokeTokensExecutor : DefenseExecutor {
    override val actionType = DefenseActionType.REVOKE_TOKENS

    override fun execute(
        alert: Alert,
        dryRun: Boolean,
    ): String = if (dryRun) "Would revoke tokens for ${alert.entityId}" else "Revoked tokens for ${alert.entityId}"
}

@Component
class DisableUserExecutor : DefenseExecutor {
    override val actionType = DefenseActionType.DISABLE_USER

    override fun execute(
        alert: Alert,
        dryRun: Boolean,
    ): String = if (dryRun) "Would disable user ${alert.entityId}" else "Disabled user ${alert.entityId}"
}
