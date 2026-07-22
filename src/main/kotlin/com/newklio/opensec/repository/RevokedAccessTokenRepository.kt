package com.newklio.opensec.repository

import com.newklio.opensec.entity.RevokedAccessToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RevokedAccessTokenRepository : JpaRepository<RevokedAccessToken, UUID> {
    fun existsByJti(jti: UUID): Boolean
}
