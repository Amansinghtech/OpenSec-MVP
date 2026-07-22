package com.newklio.opensec.service

import com.newklio.opensec.config.JWTConfig
import com.newklio.opensec.entity.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import java.util.UUID

@Component
class JWTService(
    jwtConfig: JWTConfig,
) {
    private val accessExpiration = jwtConfig.accessExpiration
    private val key = Keys.hmacShaKeyFor(jwtConfig.secret.toByteArray())

    fun generateAccessToken(user: User): String {
        val now = Date()
        return Jwts
            .builder()
            .setId(UUID.randomUUID().toString())
            .setSubject(user.username)
            .claim("tenantId", user.tenant?.id?.toString())
            .claim("roles", user.roles.map { it.name })
            .claim("type", "access")
            .setIssuedAt(now)
            .setExpiration(Date(now.time + accessExpiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    val accessTokenValidityMs: Long
        get() = accessExpiration

    fun extractUsername(token: String): String = parseClaims(token).subject

    fun extractJti(token: String): String = parseClaims(token).id

    fun extractExpiration(token: String): Instant = parseClaims(token).expiration.toInstant()

    fun validateToken(
        token: String,
        username: String,
    ): Boolean {
        val claims = parseClaims(token)
        return claims.subject == username && !claims.expiration.before(Date())
    }

    private fun parseClaims(token: String): Claims =
        Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
}
