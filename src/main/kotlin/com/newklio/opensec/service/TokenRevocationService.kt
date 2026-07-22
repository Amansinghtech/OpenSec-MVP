package com.newklio.opensec.service

import java.time.Instant
import java.util.UUID

/**
 * Abstraction over the access-token revocation list.
 *
 * Backed by the database for now; Phase 3 will introduce a Redis-backed implementation
 * (the JWT revocation list belongs in Redis per the PRD). Keeping this behind an interface
 * makes that swap a one-line change.
 */
interface TokenRevocationService {
    fun revoke(jti: UUID, expiresAt: Instant)
    fun isRevoked(jti: UUID): Boolean
}
