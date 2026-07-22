package com.newklio.opensec.controller

import com.newklio.opensec.dto.AuthResponse
import com.newklio.opensec.dto.LoginRequest
import com.newklio.opensec.dto.LogoutRequest
import com.newklio.opensec.dto.RefreshRequest
import com.newklio.opensec.dto.SignupRequest
import com.newklio.opensec.entity.AuthenticatedUser
import com.newklio.opensec.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody request: SignupRequest,
        httpRequest: HttpServletRequest
    ): AuthResponse = authService.signup(request, httpRequest)

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest
    ): AuthResponse = authService.login(request, httpRequest)

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
        httpRequest: HttpServletRequest
    ): AuthResponse = authService.refresh(request.refreshToken, httpRequest)

    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody request: LogoutRequest,
        @RequestHeader(value = "Authorization", required = false) authorization: String?,
        @AuthenticationPrincipal principal: AuthenticatedUser?,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Void> {
        val accessToken = authorization?.takeIf { it.startsWith("Bearer ") }?.substring(7)
        authService.logout(
            accessToken = accessToken,
            refreshToken = request.refreshToken,
            username = principal?.username,
            tenantId = principal?.tenantId,
            httpRequest = httpRequest
        )
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
