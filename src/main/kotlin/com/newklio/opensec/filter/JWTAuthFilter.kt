package com.newklio.opensec.filter

import com.newklio.opensec.service.JWTService
import com.newklio.opensec.service.TokenRevocationService
import com.newklio.opensec.service.UserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JWTAuthFilter(
    private val jwtService: JWTService,
    private val userDetailsService: UserService,
    private val tokenRevocationService: TokenRevocationService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.substring(7)

        runCatching {
            if (SecurityContextHolder.getContext().authentication != null) {
                return@runCatching
            }

            val jti = runCatching { UUID.fromString(jwtService.extractJti(token)) }.getOrNull()
            if (jti != null && tokenRevocationService.isRevoked(jti)) {
                return@runCatching
            }

            val username = jwtService.extractUsername(token)
            val userDetails = userDetailsService.loadUserByUsername(username)

            if (userDetails.isEnabled && jwtService.validateToken(token, userDetails.username)) {
                val authToken =
                    UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities,
                    )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }
}
