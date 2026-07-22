package com.newklio.opensec

import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.model.AlertStatus
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.SignalSeverity
import com.newklio.opensec.service.AlertService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
class AlertServiceTest
    @Autowired
    constructor(
        private val alertService: AlertService,
    ) {
        @Test
        fun `creates alert from detection signal`() {
            val tenantId = UUID.randomUUID()
            val signal =
                DetectionSignal(
                    signalId = UUID.randomUUID(),
                    tenantId = tenantId,
                    detector = "signature",
                    signalType = "brute_force",
                    entityType = EntityType.USER,
                    entityId = "admin",
                    riskScore = 90,
                    severity = SignalSeverity.CRITICAL,
                    reason = "Multiple failed logins",
                    occurredAt = Instant.now(),
                )

            val alert = alertService.createFromSignal(signal)
            assertEquals(AlertStatus.NEW, alert.status)
            assertEquals(90, alert.riskScore)
            assertEquals(1, alertService.listAlerts(tenantId).size)
        }

        @Test
        fun `updates alert status`() {
            val tenantId = UUID.randomUUID()
            val alert =
                alertService.createFromSignal(
                    DetectionSignal(
                        tenantId = tenantId,
                        detector = "anomaly",
                        signalType = "spike",
                        entityType = EntityType.HOST,
                        entityId = "host-1",
                        riskScore = 75,
                        severity = SignalSeverity.HIGH,
                        reason = "Traffic spike",
                        occurredAt = Instant.now(),
                    ),
                )

            val updated = alertService.updateStatus(alert.id, tenantId, AlertStatus.ACKNOWLEDGED)
            assertNotNull(updated)
            assertEquals(AlertStatus.ACKNOWLEDGED, updated.status)
        }
    }
