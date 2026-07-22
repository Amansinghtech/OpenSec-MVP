package com.newklio.opensec.service

import com.newklio.opensec.config.DetectionProperties
import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.DETECTION_SIGNAL_EVENT
import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.dto.SessionSummary
import com.newklio.opensec.entity.DetectionSignalRecord
import com.newklio.opensec.repository.DetectionSignalRepository
import com.newklio.opensec.service.detector.Detector
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

/**
 * Multi-layer detection/policy engine (PRD §6). Runs all detectors over an input, applies the
 * emit-threshold policy, then persists and publishes each surviving [DetectionSignal].
 */
@Service
class DetectionEngine(
    private val detectors: List<Detector>,
    private val detectionSignalRepository: DetectionSignalRepository,
    private val eventPublisher: EventPublisher,
    private val objectMapper: ObjectMapper,
    private val properties: DetectionProperties,
) {
    @Transactional
    fun evaluateEvent(event: NormalizedEvent): List<DetectionSignal> = emit(detectors.flatMap { it.onEvent(event) })

    @Transactional
    fun evaluateSession(session: SessionSummary): List<DetectionSignal> = emit(detectors.flatMap { it.onSession(session) })

    private fun emit(candidates: List<DetectionSignal>): List<DetectionSignal> {
        val accepted = candidates.filter { it.riskScore >= properties.emitThreshold }
        for (signal in accepted) {
            detectionSignalRepository.save(toRecord(signal))
            publish(signal)
        }
        return accepted
    }

    private fun publish(signal: DetectionSignal) {
        @Suppress("UNCHECKED_CAST")
        val payload = objectMapper.convertValue(signal, Map::class.java) as Map<String, Any?>
        eventPublisher.publish(
            KafkaTopics.DETECTION_SIGNALS,
            EventEnvelope(
                eventType = DETECTION_SIGNAL_EVENT,
                tenantId = signal.tenantId,
                correlationId = signal.correlationId,
                source = signal.detector,
                occurredAt = signal.occurredAt,
                payload = payload,
                eventId = signal.signalId,
            ),
        )
    }

    private fun toRecord(signal: DetectionSignal): DetectionSignalRecord =
        DetectionSignalRecord(
            id = signal.signalId,
            tenantId = signal.tenantId,
            correlationId = signal.correlationId,
            detector = signal.detector,
            signalType = signal.signalType,
            entityType = signal.entityType,
            entityId = signal.entityId,
            riskScore = signal.riskScore,
            severity = signal.severity,
            reason = signal.reason,
            sourceRef = signal.sourceRef,
            occurredAt = signal.occurredAt,
        )
}
