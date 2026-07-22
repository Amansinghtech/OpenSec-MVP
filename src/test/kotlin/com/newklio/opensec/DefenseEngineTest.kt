package com.newklio.opensec

import com.newklio.opensec.dto.Alert
import com.newklio.opensec.model.AlertStatus
import com.newklio.opensec.model.DefenseActionStatus
import com.newklio.opensec.model.DefenseActionType
import com.newklio.opensec.model.EntityType
import com.newklio.opensec.model.SignalSeverity
import com.newklio.opensec.service.DefenseEngine
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
class DefenseEngineTest
    @Autowired
    constructor(
        private val defenseEngine: DefenseEngine,
    ) {
        @Test
        fun `critical alert triggers dry-run defense actions`() {
            val tenantId = UUID.randomUUID()
            val alert =
                Alert(
                    id = UUID.randomUUID(),
                    tenantId = tenantId,
                    title = "Critical brute force",
                    description = "Attack detected",
                    entityType = EntityType.USER,
                    entityId = "admin",
                    riskScore = 95,
                    severity = SignalSeverity.CRITICAL,
                    status = AlertStatus.NEW,
                    createdAt = Instant.now(),
                )

            val actions = defenseEngine.evaluateAlert(alert)
            assertTrue(actions.isNotEmpty())
            assertTrue(actions.all { it.dryRun })
            assertTrue(actions.any { it.actionType == DefenseActionType.BLOCK_IP })
            assertEquals(DefenseActionStatus.DRY_RUN, actions.first().status)
        }
    }
