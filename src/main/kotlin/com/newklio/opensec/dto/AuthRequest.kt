package com.newklio.opensec.dto

data class SignupRequest(
    val username: String,
    val password: String,
    val email: String,
    val phoneNumber: String
)
