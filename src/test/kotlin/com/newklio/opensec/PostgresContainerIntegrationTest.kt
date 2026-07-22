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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Verifies the Flyway migrations and full auth flow against a real PostgreSQL instance.
 * Auto-skipped when Docker is unavailable (e.g. local dev without Docker); runs in CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgresContainerIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) {
        companion object {
            @Container
            @JvmStatic
            val postgres = PostgreSQLContainer("postgres:16-alpine")

            @JvmStatic
            @DynamicPropertySource
            fun datasourceProperties(registry: DynamicPropertyRegistry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl)
                registry.add("spring.datasource.username", postgres::getUsername)
                registry.add("spring.datasource.password", postgres::getPassword)
                registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            }
        }

        @Test
        fun `migrations apply and auth flow works on postgres`() {
            val body =
                """{"username":"pguser","password":"supersecret","email":"pguser@example.com","phoneNumber":"1234567890"}"""
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString

            val accessToken = JsonPath.read<String>(response, "$.accessToken")

            mockMvc
                .perform(
                    get("/api/v1/users/me").header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.username").value("pguser"))
                .andExpect(jsonPath("$.tenant.slug").value("default"))
        }
    }
