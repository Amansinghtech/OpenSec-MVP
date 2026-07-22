package com.newklio.opensec.service

import com.newklio.opensec.config.BootstrapConfig
import com.newklio.opensec.dto.AuthResponse
import com.newklio.opensec.dto.LoginRequest
import com.newklio.opensec.dto.SignupRequest
import com.newklio.opensec.entity.AuthEventType
import com.newklio.opensec.entity.User
import com.newklio.opensec.repository.RoleRepository
import com.newklio.opensec.repository.TenantRepository
import com.newklio.opensec.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

const val DEFAULT_SIGNUP_ROLE = "VIEWER"

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val tenantRepository: TenantRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JWTService,
    private val refreshTokenService: RefreshTokenService,
    private val tokenRevocationService: TokenRevocationService,
    private val authAuditService: AuthAuditService,
    private val bootstrapConfig: BootstrapConfig,
) {
    @Transactional
    fun signup(
        request: SignupRequest,
        httpRequest: HttpServletRequest,
    ): AuthResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalStateException("Username already exists")
        }

        val tenant =
            tenantRepository.findBySlug(bootstrapConfig.defaultTenantSlug)
                ?: throw IllegalStateException("Default tenant is not initialized")
        val defaultRole =
            roleRepository.findByName(DEFAULT_SIGNUP_ROLE)
                ?: throw IllegalStateException("Default role '$DEFAULT_SIGNUP_ROLE' is not initialized")

        val user =
            User(
                username = request.username,
                password = passwordEncoder.encode(request.password)!!,
                email = request.email,
                phone = request.phoneNumber,
            )
        user.tenant = tenant
        user.roles.add(defaultRole)

        val saved = userRepository.save(user)
        authAuditService.record(AuthEventType.SIGNUP, saved.username, tenant.id, httpRequest)
        return buildTokens(saved)
    }

    @Transactional
    fun login(
        request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): AuthResponse {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.username, request.password),
            )
        } catch (ex: AuthenticationException) {
            authAuditService.record(
                AuthEventType.LOGIN_FAILURE,
                request.username,
                request = httpRequest,
                detail = ex.message,
            )
            throw ex
        }

        val user =
            userRepository.findByUsername(request.username)
                ?: throw BadCredentialsException("Invalid credentials")

        authAuditService.record(AuthEventType.LOGIN_SUCCESS, user.username, user.tenant?.id, httpRequest)
        return buildTokens(user)
    }

    @Transactional
    fun refresh(
        presentedToken: String,
        httpRequest: HttpServletRequest,
    ): AuthResponse {
        val rotated =
            refreshTokenService.rotate(presentedToken)
                ?: throw BadCredentialsException("Invalid or expired refresh token")

        val user =
            userRepository
                .findById(rotated.userId)
                .orElseThrow { BadCredentialsException("Invalid or expired refresh token") }

        authAuditService.record(AuthEventType.TOKEN_REFRESH, user.username, user.tenant?.id, httpRequest)

        val accessToken = jwtService.generateAccessToken(user)
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = rotated.token,
            expiresInMs = jwtService.accessTokenValidityMs,
        )
    }

    @Transactional
    fun logout(
        accessToken: String?,
        refreshToken: String,
        username: String?,
        tenantId: UUID?,
        httpRequest: HttpServletRequest,
    ) {
        accessToken?.let {
            runCatching {
                val jti = UUID.fromString(jwtService.extractJti(it))
                tokenRevocationService.revoke(jti, jwtService.extractExpiration(it))
            }
        }
        refreshTokenService.revoke(refreshToken)
        authAuditService.record(AuthEventType.LOGOUT, username, tenantId, httpRequest)
    }

    private fun buildTokens(user: User): AuthResponse {
        val accessToken = jwtService.generateAccessToken(user)
        val refreshToken = refreshTokenService.issue(user.id!!)
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            expiresInMs = jwtService.accessTokenValidityMs,
        )
    }
}
