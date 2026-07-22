package com.newklio.opensec

import com.jayway.jsonpath.JsonPath
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import java.time.Duration
import java.util.Properties
import kotlin.test.assertTrue

/**
 * End-to-end: ingest a Wazuh alert → `raw_logs` → normalization listener → `normalized_events`.
 * Auto-skipped when Docker is unavailable; runs in CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class NormalizationPipelineKafkaTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) {
        companion object {
            @Container
            @JvmStatic
            val kafka = KafkaContainer("apache/kafka:3.8.0")

            @JvmStatic
            @DynamicPropertySource
            fun kafkaProperties(registry: DynamicPropertyRegistry) {
                registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
                registry.add("opensec.kafka.enabled") { "true" }
                registry.add("opensec.kafka.partitions") { "1" }
            }
        }

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
        fun `wazuh alert flows through to normalized_events`() {
            val token = adminToken()
            val alert =
                """
                {
                  "timestamp": "2024-01-01T12:00:00.000+0000",
                  "rule": {"id":"5710","level":8,"description":"sshd brute force","groups":["syslog","authentication_failed"]},
                  "agent": {"id":"001","name":"pipeline-host","ip":"10.0.0.9"},
                  "id": "pipeline-evt-1",
                  "data": {"srcip":"9.9.9.9"}
                }
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/ingest/wazuh")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alert),
                ).andExpect(status().isOk)

            newConsumer().use { consumer ->
                consumer.subscribe(listOf("normalized_events"))
                val deadline = System.currentTimeMillis() + 30_000
                var found = false
                while (System.currentTimeMillis() < deadline && !found) {
                    val records = consumer.poll(Duration.ofMillis(500))
                    for (record in records) {
                        if (record.value().contains("AUTH_FAILURE") && record.value().contains("9.9.9.9")) {
                            found = true
                        }
                    }
                }
                assertTrue(found, "Expected a normalized AUTH_FAILURE event for 9.9.9.9 on normalized_events")
            }
        }

        private fun newConsumer(): KafkaConsumer<String, String> {
            val props = Properties()
            props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafka.bootstrapServers
            props[ConsumerConfig.GROUP_ID_CONFIG] = "test-norm-${System.currentTimeMillis()}"
            props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            return KafkaConsumer(props)
        }
    }
