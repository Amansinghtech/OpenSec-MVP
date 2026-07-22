package com.newklio.opensec

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Verifies the Redis-backed token revocation and rate limiting against a real Redis instance.
 * Auto-skipped when Docker is unavailable (local dev without Docker); runs in CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RedisIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) {
        companion object {
            @Container
            @JvmStatic
            val redis: GenericContainer<*> = GenericContainer("redis:7-alpine").withExposedPorts(6379)

            @JvmStatic
            @DynamicPropertySource
            fun redisProperties(registry: DynamicPropertyRegistry) {
                registry.add("spring.data.redis.host", redis::getHost)
                registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
                registry.add("opensec.revocation.store") { "redis" }
                registry.add("opensec.rate-limit.enabled") { "true" }
                registry.add("opensec.rate-limit.limit") { "3" }
                registry.add("opensec.rate-limit.window-seconds") { "60" }
            }
        }

        private fun signup(username: String): Pair<String, String> {
            val body =
                """{"username":"$username","password":"supersecret","email":"$username@example.com","phoneNumber":"1234567890"}"""
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString
            return JsonPath.read<String>(response, "$.accessToken") to
                JsonPath.read<String>(response, "$.refreshToken")
        }

        @Test
        fun `logout revokes access token via redis`() {
            val (accessToken, refreshToken) = signup("redisuser")

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
        fun `login endpoint is rate limited after exceeding the budget`() {
            val body = """{"username":"nobody","password":"whatever"}"""

            // limit = 3: first three attempts are allowed through (401 bad credentials),
            // the fourth is throttled with 429.
            repeat(3) {
                mockMvc
                    .perform(
                        post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body),
                    ).andExpect(status().isUnauthorized)
            }

            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isTooManyRequests)
        }
    }
