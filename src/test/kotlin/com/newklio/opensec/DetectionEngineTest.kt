package com.newklio.opensec

import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.dto.SessionSummary
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.NormalizedEventType
import com.newklio.opensec.model.SessionStatus
import com.newklio.opensec.model.SignalSeverity
import com.newklio.opensec.repository.DetectionSignalRepository
import com.newklio.opensec.service.DetectionEngine
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class DetectionEngineTest
    @Autowired
    constructor(
        private val detectionEngine: DetectionEngine,
        private val detectionSignalRepository: DetectionSignalRepository,
    ) {
        private fun event(
            type: NormalizedEventType,
            severity: Int?,
        ): NormalizedEvent =
            NormalizedEvent(
                eventId = UUID.randomUUID(),
                tenantId = UUID.randomUUID(),
                source = "wazuh",
                eventType = type,
                category = "test",
                severity = severity,
                host = "h",
                entityType = EntityType.IP,
                entityId = "198.51.100.5",
                occurredAt = Instant.now(),
            )

        @Test
        fun `malware event produces a critical signature signal`() {
            val signals = detectionEngine.evaluateEvent(event(NormalizedEventType.MALWARE, 12))

            val signature = signals.first { it.detector == "signature" }
            assertEquals(SignalSeverity.CRITICAL, signature.severity)
            assertTrue(detectionSignalRepository.existsById(signature.signalId))
        }

        @Test
        fun `low-signal generic event below threshold emits nothing`() {
            val signals = detectionEngine.evaluateEvent(event(NormalizedEventType.GENERIC, 2))
            assertTrue(signals.isEmpty())
        }

        @Test
        fun `high severity event triggers the anomaly detector`() {
            val signals = detectionEngine.evaluateEvent(event(NormalizedEventType.NETWORK, 11))
            assertTrue(signals.any { it.detector == "anomaly" && it.riskScore >= 70 })
        }

        @Test
        fun `suspicious session produces a behavioral signal`() {
            val summary =
                SessionSummary(
                    sessionId = UUID.randomUUID(),
                    tenantId = UUID.randomUUID(),
                    entityType = EntityType.IP,
                    entityId = "198.51.100.9",
                    status = SessionStatus.OPEN,
                    eventCount = 6,
                    authFailureCount = 5,
                    suspicious = true,
                    riskReason = "possible successful brute force after 4 failures",
                    startedAt = Instant.now(),
                    lastEventAt = Instant.now(),
                )

            val signals = detectionEngine.evaluateSession(summary)

            val behavioral = signals.first { it.detector == "behavioral" }
            assertEquals(SignalSeverity.CRITICAL, behavioral.severity)
            assertTrue(detectionSignalRepository.existsById(behavioral.signalId))
        }
    }
