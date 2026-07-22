package com.newklio.opensec.service

import com.newklio.opensec.config.JWTConfig
import com.newklio.opensec.entity.RefreshToken
import com.newklio.opensec.repository.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    jwtConfig: JWTConfig,
) {
    private val refreshExpiration = jwtConfig.refreshExpiration
    private val random = SecureRandom()

    fun issue(userId: UUID): RefreshToken {
        val token =
            RefreshToken(
                token = generateOpaqueToken(),
                userId = userId,
                expiresAt = Instant.now().plusMillis(refreshExpiration),
            )
        return refreshTokenRepository.save(token)
    }

    /**
     * Validates and rotates a refresh token: the presented token is revoked and a new one
     * is issued for the same user. Returns null when the token is missing, revoked or expired.
     */
    @Transactional
    fun rotate(presentedToken: String): RefreshToken? {
        val existing = refreshTokenRepository.findByToken(presentedToken) ?: return null
        if (existing.revoked || existing.expiresAt.isBefore(Instant.now())) {
            return null
        }
        existing.revoked = true
        refreshTokenRepository.save(existing)
        return issue(existing.userId)
    }

    fun findValid(presentedToken: String): RefreshToken? {
        val existing = refreshTokenRepository.findByToken(presentedToken) ?: return null
        if (existing.revoked || existing.expiresAt.isBefore(Instant.now())) {
            return null
        }
        return existing
    }

    @Transactional
    fun revoke(presentedToken: String) {
        refreshTokenRepository.findByToken(presentedToken)?.let {
            it.revoked = true
            refreshTokenRepository.save(it)
        }
    }

    @Transactional
    fun revokeAllForUser(userId: UUID) {
        refreshTokenRepository.revokeAllForUser(userId)
    }

    private fun generateOpaqueToken(): String {
        val bytes = ByteArray(48)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
