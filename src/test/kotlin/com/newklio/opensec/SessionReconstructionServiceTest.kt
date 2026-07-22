package com.newklio.opensec

import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.NormalizedEventType
import com.newklio.opensec.service.SessionReconstructionService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class SessionReconstructionServiceTest
    @Autowired
    constructor(
        private val sessionReconstructionService: SessionReconstructionService,
    ) {
        private fun event(
            tenantId: UUID,
            entityId: String,
            type: NormalizedEventType,
            at: Instant,
        ): NormalizedEvent =
            NormalizedEvent(
                eventId = UUID.randomUUID(),
                tenantId = tenantId,
                source = "wazuh",
                eventType = type,
                severity = 5,
                host = "h",
                entityType = EntityType.IP,
                entityId = entityId,
                occurredAt = at,
            )

        @Test
        fun `events for the same entity accumulate into one session`() {
            val tenant = UUID.randomUUID()
            val ip = "203.0.113.10"
            val t0 = Instant.now()

            sessionReconstructionService.process(event(tenant, ip, NormalizedEventType.NETWORK, t0))
            val session =
                sessionReconstructionService.process(
                    event(tenant, ip, NormalizedEventType.NETWORK, t0.plusSeconds(60)),
                )

            assertEquals(2, session.eventCount)
            assertFalse(session.suspicious)
        }

        @Test
        fun `repeated auth failures flag the session as brute force`() {
            val tenant = UUID.randomUUID()
            val ip = "203.0.113.20"
            val t0 = Instant.now()

            var session = sessionReconstructionService.process(event(tenant, ip, NormalizedEventType.AUTH_FAILURE, t0))
            for (i in 1..4) {
                session =
                    sessionReconstructionService.process(
                        event(tenant, ip, NormalizedEventType.AUTH_FAILURE, t0.plusSeconds(i * 10L)),
                    )
            }

            assertEquals(5, session.authFailureCount)
            assertTrue(session.suspicious)
            assertTrue(session.riskReason!!.contains("brute force"))
        }

        @Test
        fun `failures followed by success flag a successful brute force`() {
            val tenant = UUID.randomUUID()
            val ip = "203.0.113.30"
            val t0 = Instant.now()

            sessionReconstructionService.process(event(tenant, ip, NormalizedEventType.AUTH_FAILURE, t0))
            sessionReconstructionService.process(event(tenant, ip, NormalizedEventType.AUTH_FAILURE, t0.plusSeconds(5)))
            sessionReconstructionService.process(event(tenant, ip, NormalizedEventType.AUTH_FAILURE, t0.plusSeconds(10)))
            val session =
                sessionReconstructionService.process(
                    event(tenant, ip, NormalizedEventType.AUTH_SUCCESS, t0.plusSeconds(15)),
                )

            assertTrue(session.suspicious)
            assertTrue(session.riskReason!!.contains("successful brute force"))
        }

        @Test
        fun `a gap beyond the window starts a new session`() {
            val tenant = UUID.randomUUID()
            val ip = "203.0.113.40"
            val t0 = Instant.now()

            val first = sessionReconstructionService.process(event(tenant, ip, NormalizedEventType.NETWORK, t0))
            // Default window is 30 minutes; jump well beyond it.
            val second =
                sessionReconstructionService.process(
                    event(tenant, ip, NormalizedEventType.NETWORK, t0.plus(2, ChronoUnit.HOURS)),
                )

            assertEquals(1, second.eventCount)
            assertTrue(first.id != second.id)
        }
    }
