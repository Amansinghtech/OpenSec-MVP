package com.newklio.opensec

import com.jayway.jsonpath.JsonPath
import com.newklio.opensec.repository.AgentRepository
import com.newklio.opensec.repository.RawLogRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val agentRepository: AgentRepository,
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

        @Test
        fun `admin can enroll an agent and receives api key`() {
            val token = adminToken()
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/agents")
                            .header("Authorization", "Bearer $token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """{"name":"web-01","hostname":"web-01.local","platform":"LINUX","wazuhAgentGroup":"default"}""",
                            ),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.agent.name").value("web-01"))
                    .andExpect(jsonPath("$.agent.status").value("PENDING"))
                    .andExpect(jsonPath("$.apiKey").exists())
                    .andReturn()
                    .response.contentAsString

            val apiKey = JsonPath.read<String>(response, "$.apiKey")
            assertTrue(apiKey.startsWith("opsk_"))
            assertEquals(1, agentRepository.count())
        }

        @Test
        fun `enrolled agent can heartbeat and ingest via api key`() {
            val admin = adminToken()
            val enrollResponse =
                mockMvc
                    .perform(
                        post("/api/v1/agents")
                            .header("Authorization", "Bearer $admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"name":"ingest-agent","platform":"LINUX"}"""),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString

            val apiKey = JsonPath.read<String>(enrollResponse, "$.apiKey")

            mockMvc
                .perform(
                    post("/api/v1/agents/heartbeat")
                        .header("X-Api-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"hostname":"ingest-agent.local","wazuhAgentId":"007"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.wazuhAgentId").value("007"))

            mockMvc
                .perform(
                    post("/api/v1/ingest/logs")
                        .header("Authorization", "ApiKey $apiKey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{"source":"custom","host":"ingest-agent","eventId":"agent-evt-1","payload":{"msg":"from agent"}}""",
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.duplicate").value(false))

            assertEquals(1, rawLogRepository.findAll().count { it.eventId == "agent-evt-1" })
        }

        @Test
        fun `admin can list fleet and revoke agent`() {
            val admin = adminToken()
            val beforeTotal = agentRepository.count()
            val enrollResponse =
                mockMvc
                    .perform(
                        post("/api/v1/agents")
                            .header("Authorization", "Bearer $admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"name":"revoke-me"}"""),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString

            val agentId = JsonPath.read<String>(enrollResponse, "$.agent.id")
            val apiKey = JsonPath.read<String>(enrollResponse, "$.apiKey")

            mockMvc
                .perform(
                    get("/api/v1/agents/fleet/summary")
                        .header("Authorization", "Bearer $admin"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.total").value(beforeTotal + 1))

            mockMvc
                .perform(
                    delete("/api/v1/agents/$agentId")
                        .header("Authorization", "Bearer $admin"),
                ).andExpect(status().isNoContent)

            mockMvc
                .perform(
                    post("/api/v1/ingest/logs")
                        .header("X-Api-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"source":"custom","payload":{}}"""),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        fun `duplicate agent name is rejected`() {
            val admin = adminToken()
            val body = """{"name":"dupe-agent"}"""

            mockMvc
                .perform(
                    post("/api/v1/agents")
                        .header("Authorization", "Bearer $admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isOk)

            mockMvc
                .perform(
                    post("/api/v1/agents")
                        .header("Authorization", "Bearer $admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isConflict)
        }
    }
