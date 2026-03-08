package com.newklio.opensec.controller

import com.newklio.opensec.dto.SignupRequest
import com.newklio.opensec.entity.User
import com.newklio.opensec.repository.UserRepository
import com.newklio.opensec.service.JWTService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JWTService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    data class AuthRequest(
        val username: String,
        val password: String
    )

    data class AuthResponse(
        val access_token: String
    )

    @PostMapping("/login")
    fun login(request: AuthRequest): AuthResponse {

        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                request.username,
                request.password
            )
        )

        val token = jwtService.generateToken(request.username)

        return AuthResponse(token)
    }

    @PostMapping("/signup")
    fun signup(request: SignupRequest): AuthResponse {

        if (userRepository.existsByUsername(request.username)) {
            throw RuntimeException("Username already exists")
        }

        val user = User(
            username = request.username,
            password = passwordEncoder.encode(request.password)!!,
            email = request.email,
            phone = request.phoneNumber
        )

        userRepository.save(user)

        val token = jwtService.generateToken(user.username)

        return AuthResponse(token)
    }
}