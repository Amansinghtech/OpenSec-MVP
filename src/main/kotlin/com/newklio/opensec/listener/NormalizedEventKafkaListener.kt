package com.newklio.opensec.listener

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.dto.NormalizedEvent
import com.newklio.opensec.service.SessionReconstructionService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Consumes normalized events and feeds them to session reconstruction. Only active when the event
 * bus is enabled; core logic is tested independently of Kafka in [SessionReconstructionService].
 */
@Component
@ConditionalOnProperty(name = ["opensec.kafka.enabled"], havingValue = "true")
class NormalizedEventKafkaListener(
    private val sessionReconstructionService: SessionReconstructionService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(NormalizedEventKafkaListener::class.java)

    @KafkaListener(
        topics = [KafkaTopics.NORMALIZED_EVENTS],
        groupId = "\${opensec.kafka.consumer-group:opensec}-sessions",
    )
    fun onMessage(message: String) {
        try {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            val normalized = objectMapper.convertValue(envelope.payload, NormalizedEvent::class.java)
            sessionReconstructionService.process(normalized)
        } catch (ex: Exception) {
            log.error("Failed to process normalized event for sessions: {}", ex.message, ex)
        }
    }
}
