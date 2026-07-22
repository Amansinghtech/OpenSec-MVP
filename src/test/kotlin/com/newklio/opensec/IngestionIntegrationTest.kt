package com.newklio.opensec

import com.jayway.jsonpath.JsonPath
import com.newklio.opensec.repository.RawLogRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IngestionIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val rawLogRepository: RawLogRepository,
    ) {
        private fun adminToken(): String {
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"username":"admin","password":"admin-test-password"}"""),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString
            return JsonPath.read(response, "$.accessToken")
        }

        private fun viewerToken(username: String): String {
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """{"username":"$username","password":"supersecret","email":"$username@example.com","phoneNumber":"1231231234"}""",
                            ),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString
            return JsonPath.read(response, "$.accessToken")
        }

        @Test
        fun `agent can ingest a generic log`() {
            val token = adminToken()
            mockMvc
                .perform(
                    post("/api/v1/ingest/logs")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{"source":"custom","host":"host-1","eventId":"evt-generic-1","severity":3,"payload":{"msg":"hello"}}""",
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.id").exists())
        }

        @Test
        fun `duplicate eventId is deduplicated`() {
            val token = adminToken()
            val body =
                """{"source":"custom","eventId":"evt-dupe-1","payload":{"n":1}}"""

            mockMvc
                .perform(
                    post("/api/v1/ingest/logs")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.duplicate").value(false))

            mockMvc
                .perform(
                    post("/api/v1/ingest/logs")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.duplicate").value(true))
        }

        @Test
        fun `batch ingestion counts accepted and duplicates`() {
            val token = adminToken()
            val body =
                """[
              {"source":"custom","eventId":"batch-a","payload":{}},
              {"source":"custom","eventId":"batch-b","payload":{}},
              {"source":"custom","eventId":"batch-a","payload":{}}
            ]"""

            mockMvc
                .perform(
                    post("/api/v1/ingest/logs/batch")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.accepted").value(2))
                .andExpect(jsonPath("$.duplicates").value(1))
        }

        @Test
        fun `wazuh alert is mapped and stored`() {
            val token = adminToken()
            val alert =
                """
                {
                  "timestamp": "2024-01-01T12:00:00.000+0000",
                  "rule": {"id":"5710","level":5,"description":"sshd: Attempt to login using a non-existent user","groups":["syslog","sshd"]},
                  "agent": {"id":"001","name":"web-server-1","ip":"10.0.0.5"},
                  "id": "1609459200.123456",
                  "full_log": "Jan  1 12:00:00 web-server-1 sshd[1234]: Invalid user admin",
                  "data": {"srcip":"1.2.3.4"}
                }
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/ingest/wazuh")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alert),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.duplicate").value(false))

            val stored = rawLogRepository.findAll().first { it.eventId == "1609459200.123456" }
            assertEquals("wazuh", stored.source)
            assertEquals("web-server-1", stored.host)
            assertEquals(5, stored.severity)
            assertNotNull(stored.correlationId)
        }

        @Test
        fun `viewer without agent role is forbidden`() {
            val token = viewerToken("ingestviewer")
            mockMvc
                .perform(
                    post("/api/v1/ingest/logs")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"source":"custom","payload":{}}"""),
                ).andExpect(status().isForbidden)
        }

        @Test
        fun `unauthenticated ingestion is rejected`() {
            mockMvc
                .perform(
                    post("/api/v1/ingest/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"source":"custom","payload":{}}"""),
                ).andExpect(status().isUnauthorized)
        }
    }
