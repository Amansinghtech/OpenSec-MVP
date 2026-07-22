package com.newklio.opensec.service.defense

import com.newklio.opensec.dto.Alert
import com.newklio.opensec.model.DefenseActionType

interface DefenseExecutor {
    val actionType: DefenseActionType

    fun execute(
        alert: Alert,
        dryRun: Boolean,
    ): String
}
