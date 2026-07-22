package com.newklio.opensec.listener

import com.newklio.opensec.config.KafkaTopics
import com.newklio.opensec.dto.ALERT_CREATED_EVENT
import com.newklio.opensec.dto.Alert
import com.newklio.opensec.dto.EventEnvelope
import com.newklio.opensec.service.DefenseEngine
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
@ConditionalOnProperty(name = ["opensec.kafka.enabled"], havingValue = "true")
class DefenseKafkaListener(
    private val defenseEngine: DefenseEngine,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(DefenseKafkaListener::class.java)

    @KafkaListener(
        topics = [KafkaTopics.ALERT_EVENTS],
        groupId = "\${opensec.kafka.consumer-group:opensec}-defense",
    )
    fun onAlertCreated(message: String) {
        try {
            val envelope = objectMapper.readValue(message, EventEnvelope::class.java)
            if (envelope.eventType != ALERT_CREATED_EVENT) return
            val alert = objectMapper.convertValue(envelope.payload, Alert::class.java)
            defenseEngine.evaluateAlert(alert)
        } catch (ex: Exception) {
            log.warn("Defense evaluation failed: {}", ex.message)
        }
    }
}
