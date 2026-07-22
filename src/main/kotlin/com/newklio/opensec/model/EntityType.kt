package com.newklio.opensec.model

/**
 * The kind of security entity an event is primarily attributed to. Sessions are reconstructed per
 * `(tenant, entityType, entityId)` (Phase 8).
 */
enum class EntityType {
    IP,
    USER,
    HOST,
    UNKNOWN,
}
