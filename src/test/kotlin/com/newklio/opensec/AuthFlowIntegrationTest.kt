package com.newklio.opensec

import com.jayway.jsonpath.JsonPath
import com.newklio.opensec.config.JWTConfig
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Date
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtConfig: JWTConfig,
    ) {
        private val adminPassword = "admin-test-password"

        private fun signupBody(username: String) =
            """{"username":"$username","password":"supersecret","email":"$username@example.com","phoneNumber":"1234567890"}"""

        private fun signup(username: String): Pair<String, String> {
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(username)),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString
            return JsonPath.read<String>(response, "$.accessToken") to
                JsonPath.read<String>(response, "$.refreshToken")
        }

        private fun login(
            username: String,
            password: String,
        ): String {
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"username":"$username","password":"$password"}"""),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString
            return JsonPath.read(response, "$.accessToken")
        }

        @Test
        fun `signup issues tokens and grants access to protected endpoint`() {
            val (accessToken, refreshToken) = signup("alice")

            mockMvc
                .perform(
                    get("/api/v1/users/me").header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.roles[0].name").value("VIEWER"))
                .andExpect(jsonPath("$.tenant.slug").value("default"))
                .andExpect(jsonPath("$.password").doesNotExist())

            assert(refreshToken.isNotBlank())
        }

        @Test
        fun `duplicate signup returns 409`() {
            signup("bob")

            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("bob")),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.status").value(409))
        }

        @Test
        fun `invalid signup body returns 400 with field errors`() {
            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"username":"a","password":"short","email":"not-an-email","phoneNumber":""}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isMap)
        }

        @Test
        fun `login with valid credentials returns tokens`() {
            signup("dave")
            val token = login("dave", "supersecret")
            assert(token.isNotBlank())
        }

        @Test
        fun `login with bad credentials returns 401`() {
            signup("erin")

            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"username":"erin","password":"wrongpassword"}"""),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.status").value(401))
        }

        @Test
        fun `protected endpoint without token returns 401`() {
            mockMvc
                .perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.status").value(401))
        }

        @Test
        fun `viewer is forbidden from admin-only endpoint`() {
            val (accessToken, _) = signup("frank")

            mockMvc
                .perform(
                    get("/api/v1/users").header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isForbidden)
                .andExpect(jsonPath("$.status").value(403))
        }

        @Test
        fun `admin can list users`() {
            val adminToken = login("admin", adminPassword)

            mockMvc
                .perform(
                    get("/api/v1/users").header("Authorization", "Bearer $adminToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$[0].username").exists())
        }

        @Test
        fun `refresh rotates token and old refresh token is rejected`() {
            val (_, refreshToken) = signup("grace")

            mockMvc
                .perform(
                    post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"refreshToken":"$refreshToken"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").exists())

            mockMvc
                .perform(
                    post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"refreshToken":"$refreshToken"}"""),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        fun `logout revokes the access token`() {
            val (accessToken, refreshToken) = signup("heidi")

            mockMvc
                .perform(
                    post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer $accessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"refreshToken":"$refreshToken"}"""),
                ).andExpect(status().isNoContent)

            mockMvc
                .perform(
                    get("/api/v1/users/me").header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        fun `expired token is rejected`() {
            val key = Keys.hmacShaKeyFor(jwtConfig.secret.toByteArray())
            val expiredToken =
                Jwts
                    .builder()
                    .setId(UUID.randomUUID().toString())
                    .setSubject("admin")
                    .claim("type", "access")
                    .setIssuedAt(Date(System.currentTimeMillis() - 120_000))
                    .setExpiration(Date(System.currentTimeMillis() - 60_000))
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact()

            mockMvc
                .perform(
                    get("/api/v1/users/me").header("Authorization", "Bearer $expiredToken"),
                ).andExpect(status().isUnauthorized)
        }
    }
