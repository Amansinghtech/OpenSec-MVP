package com.newklio.opensec

import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.NormalizedEventType
import com.newklio.opensec.repository.NormalizedEventRepository
import com.newklio.opensec.service.NormalizationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
class NormalizationServiceTest
    @Autowired
    constructor(
        private val normalizationService: NormalizationService,
        private val normalizedEventRepository: NormalizedEventRepository,
    ) {
        private fun wazuhEnvelope(eventId: UUID = UUID.randomUUID()): EventEnvelope =
            EventEnvelope(
                eventType = "raw_log.ingested",
                tenantId = UUID.randomUUID(),
                correlationId = "corr-1",
                source = "wazuh",
                occurredAt = Instant.now(),
                eventId = eventId,
                payload =
                    mapOf(
                        "id" to "1609459200.1",
                        "rule" to
                            mapOf(
                                "id" to "5710",
                                "level" to 5,
                                "description" to "sshd: authentication failure",
                                "groups" to listOf("syslog", "sshd", "authentication_failed"),
                            ),
                        "agent" to mapOf("id" to "001", "name" to "web-1", "ip" to "10.0.0.5"),
                        "data" to mapOf("srcip" to "1.2.3.4", "srcuser" to "root"),
                    ),
            )

        @Test
        fun `maps a wazuh auth failure to a normalized event`() {
            val normalized = normalizationService.normalize(wazuhEnvelope())

            assertEquals(NormalizedEventType.AUTH_FAILURE, normalized.eventType)
            assertEquals(EntityType.IP, normalized.entityType)
            assertEquals("1.2.3.4", normalized.entityId)
            assertEquals(5, normalized.severity)
            assertEquals("web-1", normalized.host)
            assertEquals("wazuh", normalized.source)

            val stored = normalizedEventRepository.findById(normalized.eventId).orElseThrow()
            assertEquals(NormalizedEventType.AUTH_FAILURE, stored.eventType)
            assertEquals("1.2.3.4", stored.entityId)
        }

        @Test
        fun `maps a generic custom event by common fields`() {
            val envelope =
                EventEnvelope(
                    eventType = "raw_log.ingested",
                    tenantId = UUID.randomUUID(),
                    source = "custom",
                    occurredAt = Instant.now(),
                    payload = mapOf("user" to "bob", "severity" to 2, "eventType" to "AUTH_SUCCESS"),
                )

            val normalized = normalizationService.normalize(envelope)

            assertEquals(NormalizedEventType.AUTH_SUCCESS, normalized.eventType)
            assertEquals(EntityType.USER, normalized.entityType)
            assertEquals("bob", normalized.entityId)
            assertEquals(2, normalized.severity)
        }

        @Test
        fun `normalization is idempotent for the same event id`() {
            val envelope = wazuhEnvelope()
            normalizationService.normalize(envelope)
            normalizationService.normalize(envelope)

            assertEquals(true, normalizedEventRepository.existsById(envelope.eventId))
        }
    }
