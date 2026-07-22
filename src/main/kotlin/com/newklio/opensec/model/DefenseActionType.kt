package com.newklio.opensec.model

enum class DefenseActionType {
    BLOCK_IP,
    DISABLE_USER,
    KILL_PROCESS,
    REVOKE_TOKENS,
    QUARANTINE_CONTAINER,
}

enum class DefenseActionStatus {
    DRY_RUN,
    PENDING,
    EXECUTED,
    FAILED,
}
