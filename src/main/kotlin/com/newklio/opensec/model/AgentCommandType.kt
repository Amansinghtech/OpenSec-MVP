package com.newklio.opensec.model

enum class AgentCommandType {
    RESTART_AGENT,
    RUN_ACTIVE_RESPONSE,
    ISOLATE_HOST,
}

enum class AgentCommandStatus {
    PENDING,
    DISPATCHED,
    FAILED,
    COMPLETED,
}
