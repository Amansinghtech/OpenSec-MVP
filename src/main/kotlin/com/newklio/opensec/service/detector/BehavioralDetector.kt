package com.newklio.opensec.service.detector

import com.newklio.opensec.config.DetectionProperties
import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.SessionSummary
import com.newklio.opensec.model.SignalSeverity
import org.springframework.stereotype.Component

/**
 * Behavioral template matching: turns suspicious reconstructed sessions (Phase 8) into scored
 * signals, weighting confirmed multi-stage patterns (successful brute force) highest.
 */
@Component
class BehavioralDetector(
    private val properties: DetectionProperties,
) : Detector {
    override val name = "behavioral"

    override fun onSession(session: SessionSummary): List<DetectionSignal> {
        if (!session.suspicious) return emptyList()

        val reason = session.riskReason ?: "Suspicious session"
        val score =
            if (reason.contains("successful brute force", ignoreCase = true)) {
                properties.weightSuccessfulBruteForce
            } else {
                properties.weightSuspiciousSession
            }

        return listOf(
            DetectionSignal(
                tenantId = session.tenantId,
                detector = name,
                signalType = "SUSPICIOUS_SESSION",
                entityType = session.entityType,
                entityId = session.entityId,
                riskScore = score,
                severity = SignalSeverity.fromScore(score),
                reason = reason,
                sourceRef = session.sessionId.toString(),
                occurredAt = session.lastEventAt,
            ),
        )
    }
}
