package com.newklio.opensec.listener

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.CONTEXTUALIZED_DETECTION_EVENT
import com.newklio.opensec.dto.ContextualizedDetection
import com.newklio.opensec.dto.DETECTION_SIGNAL_EVENT
import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.service.AlertService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
@ConditionalOnProperty(name = ["opensec.kafka.enabled"], havingValue = "true")
class AlertKafkaListener(
    private val alertService: AlertService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(AlertKafkaListener::class.java)

    @KafkaListener(
        topics = [KafkaTopics.DETECTION_SIGNALS],
        groupId = "\${opensec.kafka.consumer-group:opensec}-alerts",
    )
    fun onDetectionSignal(message: String) {
        try {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            if (envelope.eventType != DETECTION_SIGNAL_EVENT) return
            val signal = objectMapper.convertValue(envelope.payload, DetectionSignal::class.java)
            alertService.createFromSignal(signal)
        } catch (ex: Exception) {
            log.warn("Alert creation from signal failed: {}", ex.message)
        }
    }

    @KafkaListener(
        topics = [KafkaTopics.CONTEXTUALIZED_DETECTIONS],
        groupId = "\${opensec.kafka.consumer-group:opensec}-alerts",
    )
    fun onContextualizedDetection(message: String) {
        try {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            if (envelope.eventType != CONTEXTUALIZED_DETECTION_EVENT) return
            val detection = objectMapper.convertValue(envelope.payload, ContextualizedDetection::class.java)
            alertService.createFromContextualized(detection)
        } catch (ex: Exception) {
            log.warn("Alert creation from contextualized detection failed: {}", ex.message)
        }
    }
}
