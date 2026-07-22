package com.newklio.opensec.service

import com.newklio.opensec.entity.RevokedAccessToken
import com.newklio.opensec.repository.RevokedAccessTokenRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class DatabaseTokenRevocationService(
    private val revokedAccessTokenRepository: RevokedAccessTokenRepository
) : TokenRevocationService {

    override fun revoke(jti: UUID, expiresAt: Instant) {
        if (!revokedAccessTokenRepository.existsByJti(jti)) {
            revokedAccessTokenRepository.save(RevokedAccessToken(jti = jti, expiresAt = expiresAt))
        }
    }

    override fun isRevoked(jti: UUID): Boolean = revokedAccessTokenRepository.existsByJti(jti)
}
