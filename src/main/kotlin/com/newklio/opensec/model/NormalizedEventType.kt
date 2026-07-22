package com.newklio.opensec.model

/**
 * Coarse classification of a normalized security event. Drives session state transitions (Phase 8)
 * and signature/behavioral detection (Phase 9).
 */
enum class NormalizedEventType {
    AUTH_FAILURE,
    AUTH_SUCCESS,
    ACCOUNT_CHANGE,
    PRIVILEGE_ESCALATION,
    FILE_INTEGRITY,
    INTRUSION,
    MALWARE,
    NETWORK,
    PROCESS,
    WEB_ATTACK,
    GENERIC,
}
