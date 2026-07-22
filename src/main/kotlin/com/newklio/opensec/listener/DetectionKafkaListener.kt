package com.newklio.opensec.listener

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.dto.SUSPICIOUS_SESSION_EVENT
import com.newklio.opensec.dto.SessionSummary
import com.newklio.opensec.service.DetectionEngine
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Feeds normalized events and suspicious sessions into the detection engine. Only active when the
 * event bus is enabled; the engine itself is tested independently of Kafka.
 */
@Component
@ConditionalOnProperty(name = ["opensec.kafka.enabled"], havingValue = "true")
class DetectionKafkaListener(
    private val detectionEngine: DetectionEngine,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(DetectionKafkaListener::class.java)

    @KafkaListener(
        topics = [KafkaTopics.NORMALIZED_EVENTS],
        groupId = "\${opensec.kafka.consumer-group:opensec}-detection",
    )
    fun onNormalizedEvent(message: String) {
        try {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            val event = objectMapper.convertValue(envelope.payload, NormalizedEvent::class.java)
            detectionEngine.evaluateEvent(event)
        } catch (ex: Exception) {
            log.error("Detection failed for normalized event: {}", ex.message, ex)
        }
    }

    @KafkaListener(
        topics = [KafkaTopics.SESSION_EVENTS],
        groupId = "\${opensec.kafka.consumer-group:opensec}-detection",
    )
    fun onSessionEvent(message: String) {
        try {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            if (envelope.eventType != SUSPICIOUS_SESSION_EVENT) return
            val session = objectMapper.convertValue(envelope.payload, SessionSummary::class.java)
            detectionEngine.evaluateSession(session)
        } catch (ex: Exception) {
            log.error("Detection failed for session event: {}", ex.message, ex)
        }
    }
}
