package com.newklio.opensec.service.detector

import com.newklio.opensec.config.DetectionProperties
import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.model.NormalizedEventType
import com.newklio.opensec.model.SignalSeverity
import org.springframework.stereotype.Component

/**
 * Rule/signature-based detection: maps known-bad event types to a base risk score.
 */
@Component
class SignatureDetector(
    private val properties: DetectionProperties,
) : Detector {
    override val name = "signature"

    override fun onEvent(event: NormalizedEvent): List<DetectionSignal> {
        val score =
            when (event.eventType) {
                NormalizedEventType.MALWARE -> properties.weightMalware
                NormalizedEventType.INTRUSION -> properties.weightIntrusion
                NormalizedEventType.WEB_ATTACK -> properties.weightWebAttack
                NormalizedEventType.PRIVILEGE_ESCALATION -> properties.weightPrivilegeEscalation
                NormalizedEventType.AUTH_FAILURE -> properties.weightAuthFailure
                else -> 0
            }
        if (score <= 0) return emptyList()

        return listOf(
            DetectionSignal(
                tenantId = event.tenantId,
                correlationId = event.correlationId,
                detector = name,
                signalType = event.eventType.name,
                entityType = event.entityType,
                entityId = event.entityId,
                riskScore = score,
                severity = SignalSeverity.fromScore(score),
                reason = "Signature match for ${event.eventType} (${event.category ?: "n/a"})",
                sourceRef = event.eventId.toString(),
                occurredAt = event.occurredAt,
            ),
        )
    }
}
