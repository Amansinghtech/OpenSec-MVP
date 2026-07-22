package com.newklio.opensec.listener

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.DETECTION_SIGNAL_EVENT
import com.newklio.opensec.dto.DetectionSignal
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.service.RagEnrichmentService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
@ConditionalOnProperty(name = ["opensec.kafka.enabled"], havingValue = "true")
class RagKafkaListener(
    private val ragEnrichmentService: RagEnrichmentService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(RagKafkaListener::class.java)

    @KafkaListener(
        topics = [KafkaTopics.DETECTION_SIGNALS],
        groupId = "\${opensec.kafka.consumer-group:opensec}-rag",
    )
    fun onDetectionSignal(message: String) {
        try {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            if (envelope.eventType != DETECTION_SIGNAL_EVENT) return
            val signal = objectMapper.convertValue(envelope.payload, DetectionSignal::class.java)
            ragEnrichmentService.enrich(signal)
        } catch (ex: Exception) {
            log.warn("RAG listener failed: {}", ex.message)
        }
    }
}
