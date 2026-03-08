package com.newklio.opensec.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*

@Component
class JWTService {

    private val secret = "very-secret-key-for-jwt-example"
    private val expiration = 86400000

    val key = Keys.secretKeyFor(SignatureAlgorithm.HS256)

    fun generateToken(username: String): String {

        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun extractUsername(token: String): String {
        return parseClaims(token).subject
    }

    fun validateToken(token: String, username: String): Boolean {
        val claims = parseClaims(token)
        return claims.subject == username && !claims.expiration.before(Date())
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
}