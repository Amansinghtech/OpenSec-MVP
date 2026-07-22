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
import java.util.UUID
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

        private fun uniqueName(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

        private fun enrollAgent(
            adminToken: String,
            name: String,
            extraFields: String = "",
        ): Pair<String, String> {
            val fields =
                if (extraFields.isBlank()) {
                    """{"name":"$name"}"""
                } else {
                    """{"name":"$name",$extraFields}"""
                }
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/agents")
                            .header("Authorization", "Bearer $adminToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fields),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.agent.name").value(name))
                    .andExpect(jsonPath("$.apiKey").exists())
                    .andReturn()
                    .response.contentAsString
            return JsonPath.read<String>(response, "$.agent.id") to JsonPath.read(response, "$.apiKey")
        }

        @Test
        fun `admin can enroll an agent and receives api key`() {
            val token = adminToken()
            val name = uniqueName("web")
            val (_, apiKey) =
                enrollAgent(
                    token,
                    name,
                    """"hostname":"web-01.local","platform":"LINUX","wazuhAgentGroup":"default"""",
                )

            assertTrue(apiKey.startsWith("opsk_"))
            assertTrue(agentRepository.findByTenantId(agentRepository.findAll().first().tenantId).any { it.name == name })
        }

        @Test
        fun `enrolled agent can heartbeat and ingest via api key`() {
            val admin = adminToken()
            val (_, apiKey) = enrollAgent(admin, uniqueName("ingest"), """"platform":"LINUX"""")

            mockMvc
                .perform(
                    post("/api/v1/agents/heartbeat")
                        .header("X-Api-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"hostname":"ingest-agent.local","wazuhAgentId":"007"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.wazuhAgentId").value("007"))

            // Empty body is accepted for heartbeat.
            mockMvc
                .perform(
                    post("/api/v1/agents/heartbeat")
                        .header("X-Api-Key", apiKey),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("ACTIVE"))

            val eventId = "agent-evt-${UUID.randomUUID()}"
            mockMvc
                .perform(
                    post("/api/v1/ingest/logs")
                        .header("Authorization", "ApiKey $apiKey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{"source":"custom","host":"ingest-agent","eventId":"$eventId","payload":{"msg":"from agent"}}""",
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.duplicate").value(false))

            assertTrue(rawLogRepository.findAll().any { it.eventId == eventId })
        }

        @Test
        fun `admin can list fleet and revoke agent`() {
            val admin = adminToken()
            val name = uniqueName("revoke")
            val (agentId, apiKey) = enrollAgent(admin, name)

            mockMvc
                .perform(
                    get("/api/v1/agents/fleet/summary")
                        .header("Authorization", "Bearer $admin"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.total").isNumber)

            mockMvc
                .perform(
                    get("/api/v1/agents/$agentId")
                        .header("Authorization", "Bearer $admin"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value(name))

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

            // Heartbeat must not resurrect a revoked agent.
            mockMvc
                .perform(
                    post("/api/v1/agents/heartbeat")
                        .header("X-Api-Key", apiKey),
                ).andExpect(status().isUnauthorized)

            mockMvc
                .perform(
                    get("/api/v1/agents/$agentId")
                        .header("Authorization", "Bearer $admin"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("REVOKED"))
        }

        @Test
        fun `duplicate agent name is rejected`() {
            val admin = adminToken()
            val name = uniqueName("dupe")
            enrollAgent(admin, name)

            mockMvc
                .perform(
                    post("/api/v1/agents")
                        .header("Authorization", "Bearer $admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"$name"}"""),
                ).andExpect(status().isConflict)
        }

        @Test
        fun `viewer without agent write cannot enroll`() {
            val signup =
                mockMvc
                    .perform(
                        post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """{"username":"agentviewer-${UUID.randomUUID().toString().take(
                                    8,
                                )}","password":"supersecret","email":"viewer@example.com","phoneNumber":"1231231234"}""",
                            ),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString
            val token = JsonPath.read<String>(signup, "$.accessToken")

            mockMvc
                .perform(
                    post("/api/v1/agents")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"${uniqueName("forbidden")}"}"""),
                ).andExpect(status().isForbidden)
        }
    }
