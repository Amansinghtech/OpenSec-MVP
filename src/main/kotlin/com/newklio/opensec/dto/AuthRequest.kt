package com.newklio.opensec.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "username is required")
    val username: String,

    @field:NotBlank(message = "password is required")
    val password: String
)

data class AuthResponse(
    val accessToken: String
)

data class SignupRequest(
    @field:NotBlank(message = "username is required")
    @field:Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
    val username: String,

    @field:NotBlank(message = "password is required")
    @field:Size(min = 8, message = "password must be at least 8 characters")
    val password: String,

    @field:Email(message = "email must be a valid email address")
    val email: String,

    @field:NotBlank(message = "phoneNumber is required")
    @field:Size(max = 15, message = "phoneNumber must be at most 15 characters")
    val phoneNumber: String
)
