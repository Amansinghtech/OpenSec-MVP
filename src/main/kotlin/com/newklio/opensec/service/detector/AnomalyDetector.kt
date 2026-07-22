package com.newklio.opensec.service.detector

import com.newklio.opensec.config.DetectionProperties
import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.model.SignalSeverity
import org.springframework.stereotype.Component

/**
 * Severity-based anomaly scoring: high-severity events (per the source's own scale) produce a
 * proportional risk score even when no specific signature matched.
 */
@Component
class AnomalyDetector(
    private val properties: DetectionProperties,
) : Detector {
    override val name = "anomaly"

    override fun onEvent(event: NormalizedEvent): List<DetectionSignal> {
        val severity = event.severity ?: return emptyList()
        if (severity < properties.anomalySeverityThreshold) return emptyList()

        val score = (severity * 10).coerceAtMost(100)
        return listOf(
            DetectionSignal(
                tenantId = event.tenantId,
                correlationId = event.correlationId,
                detector = name,
                signalType = "HIGH_SEVERITY_ANOMALY",
                entityType = event.entityType,
                entityId = event.entityId,
                riskScore = score,
                severity = SignalSeverity.fromScore(score),
                reason = "High-severity event (level $severity) for ${event.entityId}",
                sourceRef = event.eventId.toString(),
                occurredAt = event.occurredAt,
            ),
        )
    }
}
